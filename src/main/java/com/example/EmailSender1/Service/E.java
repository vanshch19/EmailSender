// package com.example.EmailSender1.Service;

// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
// import org.springframework.stereotype.Service;

// import com.example.EmailSender1.Model.EmailConfig;

// import javax.mail.*;
// import javax.mail.internet.*;
// import java.util.*;
// import java.util.concurrent.*;

// @Service
// public class E {
//     @Autowired
//     private ThreadPoolTaskScheduler scheduler;

//     private final Map<String, ScheduledFuture<?>> taskMap = new ConcurrentHashMap<>();
//     private final Map<String, List<String>> resultMap = new ConcurrentHashMap<>();

//     @SuppressWarnings("deprecation")
//     public String startEmailSending(List<String> senderEmails, String senderName, int sendInterval,
//                                     String timeUnit, List<EmailConfig> emailConfigs, boolean ssl, String sendMode,
//                                     List<String> toEmails, String subject, String messageText, String plainTextMessage) {

//         String processId = UUID.randomUUID().toString();

//         List<String> sentEmails = Collections.synchronizedList(new ArrayList<>());
//         List<String> failedEmails = Collections.synchronizedList(new ArrayList<>());

//         CountDownLatch latch = new CountDownLatch(toEmails.size());

//         for (int i = 0; i < toEmails.size(); i++) {
//             String recipientEmail = toEmails.get(i);
//             String currentSenderEmail = senderEmails.get(i % senderEmails.size()); // Round-robin sender emails
//             String username = senderEmails.get(i % senderEmails.size()); // Round-robin sender emails
//             EmailConfig emailConfig = emailConfigs.get(i % emailConfigs.size()); // Round-robin email config

//             // Set up the SMTP properties
//             Properties props = new Properties();
//             props.put("mail.smtp.auth", emailConfig.isAuthenticationRequired() ? "true" : "false");
//             props.put("mail.smtp.starttls.enable", "true");
//             props.put("mail.smtp.host", emailConfig.getSmtpServer());
//             props.put("mail.smtp.port", emailConfig.getPort());

//             if (ssl) {
//                 props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
//                 props.put("mail.smtp.socketFactory.port", emailConfig.getPort());
//             }

//             Session session = Session.getInstance(props, new Authenticator() {
//                 @Override
//                 protected PasswordAuthentication getPasswordAuthentication() {
//                     return new PasswordAuthentication(username, emailConfig.getPassword());
//                 }
//             });

//             ScheduledFuture<?> future = scheduler.schedule(() -> {
//                 try {
//                     sendEmailInternal(session, currentSenderEmail, recipientEmail, subject, messageText, plainTextMessage);
//                     sentEmails.add("Sent message successfully from " + currentSenderEmail + " to " + recipientEmail);
//                 } catch (Exception e) {
//                     failedEmails.add("Failed to send message from " + currentSenderEmail + " to " + recipientEmail + " because " + e.getMessage());
//                 } finally {
//                     latch.countDown();
//                 }
//             }, new Date(System.currentTimeMillis() + (i * convertToMilliseconds(sendInterval, timeUnit))));

//             taskMap.put(processId, future);
//         }

//         // Wait for all tasks to complete or until a cancellation request is received
//         new Thread(() -> {
//             try {
//                 latch.await();
//                 resultMap.put(processId, Arrays.asList("Sent Emails: " + sentEmails, "Failed Emails: " + failedEmails));
//             } catch (InterruptedException e) {
//                 Thread.currentThread().interrupt();
//             }
//         }).start();

//         return processId;
//     }

//     public Map<String, List<String>> cancelEmailSending(String processId) {
//         ScheduledFuture<?> future = taskMap.remove(processId);
//         if (future != null) {
//             future.cancel(true); // Attempt to cancel the task
//         }
//         return Collections.singletonMap("results", resultMap.remove(processId));
//     }

//     private void sendEmailInternal(Session session, String from, String toEmail, String subject, String messageText, String plainTextMessage) throws MessagingException {
//         Message message = new MimeMessage(session);
//         message.setFrom(new InternetAddress(from));
//         message.setRecipient(Message.RecipientType.TO, new InternetAddress(toEmail));
//         message.setSubject(subject);

//         boolean isHtml = messageText != null &&
//                 (messageText.contains("<html>") ||
//                         messageText.contains("<title>") ||
//                         messageText.contains("<body>") ||
//                         messageText.contains("<table>"));

//         if (isHtml) {
//             Multipart multipart = new MimeMultipart();
//             BodyPart htmlPart = new MimeBodyPart();
//             htmlPart.setContent(messageText, "text/html; charset=UTF-8");
//             multipart.addBodyPart(htmlPart);

//             BodyPart plainTextPart = new MimeBodyPart();
//             plainTextPart.setText(plainTextMessage);
//             multipart.addBodyPart(plainTextPart);

//             message.setContent(multipart);
//         } else {
//             message.setText(plainTextMessage);
//         }

//         Transport.send(message);
//     }

//     private long convertToMilliseconds(int sendInterval, String timeUnit) {
//         long intervalMillis;
//         switch (timeUnit) {
//             case "seconds":
//                 intervalMillis = sendInterval * 1000;
//                 break;
//             case "minutes":
//                 intervalMillis = sendInterval * 60000;
//                 break;
//             case "hours":
//                 intervalMillis = sendInterval * 3600000;
//                 break;
//             default:
//                 intervalMillis = 0;
//         }
//         return intervalMillis;
//     }
// }






















package com.example.EmailSender1.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;

import com.example.EmailSender1.Model.EmailConfig;

import javax.mail.*;

import java.util.concurrent.CountDownLatch;

@Service
public class E {
    @Autowired
    private ThreadPoolTaskScheduler scheduler;

    @SuppressWarnings("deprecation")
    public Map<String, List<String>> sendEmail(
            List<String> senderEmails,
            List<String> senderPasswords,
            String senderName, int sendInterval, String timeUnit,
            List<EmailConfig> emailConfigs, boolean ssl, String sendMode,
            List<String> toEmails, String subject, String messageText, String plainTextMessage,
            List<String> sentEmails, List<String> failedEmails) {

        // Convert send interval to milliseconds
        long intervalMillis = convertToMilliseconds(sendInterval, timeUnit);

        // Prepare the result to be sent back to the frontend
        Map<String, List<String>> result = new HashMap<>();
        
        // Use CountDownLatch to wait for all tasks to complete
        CountDownLatch latch = new CountDownLatch(toEmails.size());

        for (int i = 0; i < toEmails.size(); i++) {
            String recipientEmail = toEmails.get(i);
            String currentSenderEmail = senderEmails.get(i % senderEmails.size()); // Round-robin sender emails
            String currentSenderPassword = senderPasswords.get(i % senderPasswords.size()); // Round-robin sender passwords
            String username = senderEmails.get(i % senderEmails.size()); // Round-robin sender emails
            EmailConfig emailConfig = emailConfigs.get(i % emailConfigs.size()); // Round-robin email config

            // Set up the SMTP properties
            Properties props = new Properties();
            props.put("mail.smtp.auth", emailConfig.isAuthenticationRequired() ? "true" : "false");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.host", emailConfig.getSmtpServer());
            props.put("mail.smtp.port", emailConfig.getPort());

            if (ssl) {
                props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
                props.put("mail.smtp.socketFactory.port", emailConfig.getPort());
            }

            // Create a session with the SMTP properties and authentication
            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(username, currentSenderPassword);
                }
            });

            scheduler.schedule(() -> {
                try {
                    sendEmailInternal(session, currentSenderEmail, recipientEmail, subject, messageText, plainTextMessage);
                    // Add successful email to the sent list
                    System.out.println("Sent message successfully to " + recipientEmail);
                    synchronized (sentEmails) {
                        sentEmails.add("Sent message successfully from " + currentSenderEmail + " to " + recipientEmail);
                    }
                } catch (Exception e) {
                    System.out.println("Failed to send message from " + currentSenderEmail + " to " + recipientEmail + " because " + e.getMessage());
                    // Add failed email to the failed list with the error message
                    synchronized (failedEmails) {
                        failedEmails.add("Failed to send message from " + currentSenderEmail + " to " + recipientEmail + " because " + e.getMessage());
                    }
                } finally {
                    latch.countDown(); // Decrement the latch count when done
                }
            }, new Date(System.currentTimeMillis() + (i * intervalMillis))); // Ensure tasks are spaced out

        }

        try {
            latch.await(); // Wait for all tasks to complete
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            // Handle the interruption appropriately
        }

        result.put("sentEmails", sentEmails);
        result.put("failedEmails", failedEmails);

        return result;
    }

    private void sendEmailInternal(Session session, String from, String toEmail,
                                    String subject, String messageText, String plainTextMessage) throws MessagingException {
        // Create a message
        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(from));
        message.setRecipient(Message.RecipientType.TO, new InternetAddress(toEmail));
        message.setSubject(subject);

        // Check if messageText is HTML
        boolean isHtml = messageText != null &&
                (messageText.contains("<html>") ||
                        messageText.contains("<title>") ||
                        messageText.contains("<body>") ||
                        messageText.contains("<table>"));

        // Set the content
        if (isHtml) {
            Multipart multipart = new MimeMultipart();

            // HTML part
            BodyPart htmlPart = new MimeBodyPart();
            htmlPart.setContent(messageText, "text/html; charset=UTF-8");
            multipart.addBodyPart(htmlPart);

            // Plain text part
            BodyPart plainTextPart = new MimeBodyPart();
            plainTextPart.setText(plainTextMessage);
            multipart.addBodyPart(plainTextPart);

            message.setContent(multipart);
        } else {
            // If plain text only, set as plain text
            message.setText(plainTextMessage);
        }

        // Send the message
        Transport.send(message);
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




