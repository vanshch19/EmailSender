package com.example.EmailSender1.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.EmailSender1.Model.DomainEmails;
import com.example.EmailSender1.Model.EmailConfig;
import com.example.EmailSender1.Model.EmailDetails;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.example.EmailSender1.Service.E;


@RestController
public class A {

    @Autowired
    private ThreadPoolTaskScheduler scheduler;

    @Autowired
    private E emailService; // Injecting the 'E' service

    @Autowired
    private MongoTemplate senderEmailsMongoTemplate; // For 'senderEmails' database

    @SuppressWarnings("deprecation")
    @PostMapping("/sendemailsToA")
    public ResponseEntity<Map<String, List<String>>> sendEmail(@RequestBody JsonNode requestData) {
        List<String> sentEmails = new ArrayList<>();
        List<String> failedEmails = new ArrayList<>();

        try {
            // Extract data from the request
            String distributionList = requestData.get("distributionList").asText();
            String segmentList = requestData.get("segmentList").asText();
            String senderEmail = requestData.has("senderEmail") ? requestData.get("senderEmail").asText() : null;
            String senderName = requestData.get("senderName").asText();
            int sendInterval = requestData.has("sendInterval") ? requestData.get("sendInterval").asInt() : 0;
            String timeUnit = requestData.get("timeUnit").asText();
            String smtpServer = requestData.has("smtpServer") ? requestData.get("smtpServer").asText() : null;
            boolean authenticationRequired = requestData.has("authenticationRequired") ? requestData.get("authenticationRequired").asBoolean() : false;
            String username = requestData.has("username") ? requestData.get("username").asText() : null;
            String password = requestData.has("password") ? requestData.get("password").asText() : null;
            String port = requestData.has("port") ? requestData.get("port").asText() : null;
            boolean ssl = requestData.has("ssl") ? requestData.get("ssl").asBoolean() : false;
            String sendMode = requestData.get("sendMode").asText();
            String subject = requestData.get("subject").asText();
            String messageText = requestData.get("message").asText();
            String plainTextMessage = requestData.get("plainTextMessage").asText();

            // Extract the 'to' array
            ArrayNode toArray = (ArrayNode) requestData.get("to");
            List<String> toEmails = new ArrayList<>();
            if (toArray != null) {
                for (JsonNode node : toArray) {
                    toEmails.add(node.asText());
                }
            }

            // Fetch domain data from DB
            List<DomainEmails> domainEmailsList = fetchDomainEmailsFromDB();
            // System.out.println(domainEmailsList.toString());

            List<EmailDetails> emailDetailsObj = extractEmailDetailsInRoundRobin(domainEmailsList);
            // System.out.println(emailDetailsObj);

           

    // Prepare lists for sender emails and email configs
    List<String> senderEmails = (senderEmail == null || senderEmail.isEmpty())
            ? extractEmailFromRoundRobinEmailList(extractEmailDetailsInRoundRobin(domainEmailsList))
            : List.of(senderEmail);

            // System.out.println(senderEmails);
            // System.out.println(senderEmails.size());

    List<String> senderPasswords = (password == null || password.isEmpty())
            ? extractPasswordFromRoundRobinEmailList(extractEmailDetailsInRoundRobin(domainEmailsList))
            : List.of(password);

            // System.out.println(senderPasswords);
            // System.out.println(senderPasswords.size());

    List<EmailConfig> emailConfigs = ((smtpServer == null || smtpServer.isEmpty()) || (port == null || port.isEmpty()) || (username == null || username.isEmpty()) || (password == null || password.isEmpty()))
            ? extractEmailConfigs(extractEmailDetailsInRoundRobin(domainEmailsList))
            : List.of(new EmailConfig(smtpServer, port, username, authenticationRequired));

            Map<String, List<String>> result = new HashMap<>();

            try {
                        // Send the email using the round-robin sender email and configuration
                        result = emailService.sendEmail( 
                            senderEmails,
                            senderPasswords,
                            senderName, 
                            sendInterval, 
                            timeUnit, 
                            emailConfigs,
                            ssl, 
                            sendMode, 
                            toEmails,
                            subject, 
                            messageText, 
                            plainTextMessage,
                            sentEmails, 
                            failedEmails
                        );

                    // Log success
                        // sentEmails.add("Sent message successfully to " + recipientEmail);

                    } catch (Exception e) {
                        // Log failure
                         // failedEmails.add("Failed to send message to " + recipientEmail + " Because " + e.getMessage()); 
                     }
 
             System.out.println(result);
 
            //  return ResponseEntity.ok("Emails scheduled successfully!");
             return ResponseEntity.ok(result);
         } catch (Exception e) {
             e.printStackTrace();
             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
         }
     }



    private List<DomainEmails> fetchDomainEmailsFromDB() {
        // Fetch all domain emails from the database
        Query query = new Query();
        List<DomainEmails> domainEmailsList = senderEmailsMongoTemplate.find(query, DomainEmails.class, "SenderEmails");

        // System.out.println(domainEmailsList);
        return domainEmailsList;
    }


    private List<EmailDetails> extractEmailDetailsInRoundRobin(List<DomainEmails> domainEmailsList) {
        List<EmailDetails> emailDetailsList = new ArrayList<>();
        boolean emailsRemaining = true;
        int emailIndex = 0;

        // Round-robin extraction of emails
        while (emailsRemaining) {
            emailsRemaining = false; // Assume no emails are remaining for this round

            for (DomainEmails domainEmails : domainEmailsList) {
                List<String> emails = domainEmails.getEmails();
                List<String> passwordList = domainEmails.getPassword();

                // Check if there is an email at the current index
                if (emailIndex < emails.size()) {
                    String email = emails.get(emailIndex);
                    String password = passwordList.get(emailIndex);

                    // Create an EmailDetails object for each email with corresponding SMTP settings
                    EmailDetails emailDetails = new EmailDetails(
                        email,
                        domainEmails.getSmtpServer(),
                        domainEmails.getPort(),
                        domainEmails.getUsername(),
                        password,
                        // domainEmails.getPassword(),
                        domainEmails.isAuthenticationRequired()
                    );
                    emailDetailsList.add(emailDetails);

                    emailsRemaining = true; // There are still emails to process in the next round
                }
            }

            emailIndex++; // Move to the next email index for the next round
        }
        // System.out.println(emailDetailsList);
        return emailDetailsList;
    }

    private List<String> extractEmailFromRoundRobinEmailList(List<EmailDetails> emailDetails){
        List<String> emails = new ArrayList<>();
        for(int i = 0;i<emailDetails.size();i++){
            emails.add(emailDetails.get(i).getEmail());
        }
        // System.out.println(emails);
        return emails;
    }

    private List<String> extractPasswordFromRoundRobinEmailList(List<EmailDetails> emailDetails){
        List<String> passwords = new ArrayList<>();
        for(int i = 0;i<emailDetails.size();i++){
            passwords.add(emailDetails.get(i).getPassword());
        }
        // System.out.println(emails);
        return passwords;
    }


    public List<EmailConfig> extractEmailConfigs(List<EmailDetails> emailDetailsList) {
    List<EmailConfig> emailConfigList = new ArrayList<>();

    for (EmailDetails emailDetail : emailDetailsList) {
        String smtpServer = emailDetail.getSmtpServer();
        String port = emailDetail.getPort();
        String username = emailDetail.getUsername();
        String password = emailDetail.getPassword();
        boolean authenticationRequired = emailDetail.isAuthenticationRequired();

        // Create EmailConfig object
        EmailConfig emailConfig = new EmailConfig(smtpServer, port, username, authenticationRequired);

        // Add the config to the list, preserving the order
        emailConfigList.add(emailConfig);
    }
    // System.out.println(emailConfigList);
    return emailConfigList;
}






private Long convertToMilliseconds(int sendInterval, String timeUnit) {
    long intervalMillis;
    switch (timeUnit) {
        case "seconds":
            intervalMillis = sendInterval * 1000;
            break;
        case "minutes":
            intervalMillis = sendInterval * 60000;
            break;
        case "hours":
            intervalMillis = sendInterval * 3600000;
            break;
        default:
            intervalMillis = 0;
    }
    return intervalMillis;
}
}
