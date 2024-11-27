package com.example.EmailSender1.Service;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.CellValue;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.EmailSender1.Model.DomainEmails;
import com.example.EmailSender1.Model.EmailConfig;
import com.example.EmailSender1.Model.EmailDetails;
import com.example.EmailSender1.Model.User;
import com.example.EmailSender1.Repository.UserRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import jakarta.servlet.http.HttpServletRequest;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.security.auth.Subject;



@RestController
public class FileUploadController {

    @Autowired
    private ThreadPoolTaskScheduler scheduler;

    // @Autowired
    // private UploadEmailService uploadEmailService; // Service to handle file processing

    private List<String> tableHeaders = new ArrayList<>();

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private MongoTemplate senderEmailsMongoTemplate; // For 'senderEmails' database

    @PostMapping("/upload")
    @ResponseBody
    public Map<String, Object> handleFileUpload(@RequestParam("file") MultipartFile file,
                                                @RequestParam("collectionName") String collectionName) {
        Map<String, Object> response = new HashMap<>();

        if (file.isEmpty()) {
            response.put("message", "Failed to upload file because it was empty.");
            return response;
        }

        List<Map<String, Object>> tableData = new ArrayList<>();

        try {
            // Extract headers and data from the uploaded file
            extractTableHeaders(file, tableHeaders);
            extractTableData(file, tableData);

            // Drop the collection if you want to replace the existing data
            mongoTemplate.dropCollection(collectionName);

            // Create a User object with the list of maps and save it
            User user = new User(tableData);
            mongoTemplate.insert(user, collectionName);

            // Fetch data from MongoDB and add to response
            response.put("tableData", fetchDataFromDb(collectionName));
            response.put("tableHeaders", tableHeaders);
            response.put("message", "File uploaded and data saved and fetched successfully from collection: " + collectionName);
        } catch (IOException e) {
            e.printStackTrace();
            response.put("message", "Failed to upload file due to an error: " + e.getMessage());
        }
        // System.out.println(response);
        return response;
    }

    // Delete functionality
    @DeleteMapping("/collections/{collectionName}")
    @ResponseBody
    public Map<String, Object> deleteCollection(@PathVariable String collectionName) {
        Map<String, Object> response = new HashMap<>();
        try {
            mongoTemplate.dropCollection(collectionName);
            response.put("message", "Collection deleted successfully.");
        } catch (Exception e) {
            e.printStackTrace();
            response.put("message", "Failed to delete collection: " + e.getMessage());
        }
        return response;
    }


    // Save functionality
    @PostMapping("/save")
    @ResponseBody
    public Map<String, Object> saveEditedData(@RequestParam("tableData") String tableData,
                                              @RequestParam("collectionName") String collectionName) {
        Map<String, Object> response = new HashMap<>();

        try {
            // Parse the JSON data received from the frontend
            List<Map<String, Object>> editedData = new ObjectMapper().readValue(tableData, new TypeReference<List<Map<String, Object>>>() {});

            // Drop the existing collection to replace the data
            mongoTemplate.dropCollection(collectionName);

            // Create a User object with the updated data and save it
            User user = new User(editedData);
            mongoTemplate.insert(user, collectionName);

            // Fetch the updated data from MongoDB
            response.put("tableData", fetchDataFromDb(collectionName));
            response.put("tableHeaders", tableHeaders);
            response.put("message", "Data updated and saved successfully!");
        } catch (IOException e) {
            e.printStackTrace();
            response.put("message", "Failed to save data due to an error: " + e.getMessage());
        }

        return response;
    }


    @PostMapping("/uploademails")
    public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file) {
        try (InputStream is = file.getInputStream()) {
            Workbook workbook = new XSSFWorkbook(is);
            Sheet sheet = workbook.getSheetAt(0);

            Map<String, DomainEmails> domainEmailsMap = new LinkedHashMap<>();
            // System.out.println(domainEmailsMap);

            for (Row row : sheet) {
                if (row.getRowNum() == 0) continue; // Skip header row
            
                String domainName = row.getCell(0).getStringCellValue();
                String email = row.getCell(1).getStringCellValue();
                String smtpServer = row.getCell(2).getStringCellValue();
                String port = Integer.toString((int) row.getCell(3).getNumericCellValue());
                String username = row.getCell(4).getStringCellValue();
                String password = row.getCell(5).getStringCellValue();
                boolean authenticationRequired = Boolean.parseBoolean(row.getCell(6).getStringCellValue());
            
                // Retrieve or create the DomainEmails object
                DomainEmails domainEmails = domainEmailsMap.computeIfAbsent(
                    domainName, 
                    k -> new DomainEmails(domainName, new ArrayList<>(), smtpServer, port, username, new ArrayList<>(), authenticationRequired)
                );
            
                // Add email and password to their respective lists
                domainEmails.getEmails().add(email);
                domainEmails.getPassword().add(password);
            }
            


        
            // Convert to a list and save to MongoDB
            List<DomainEmails> domainEmailsList = new ArrayList<>(domainEmailsMap.values());
            // System.out.println(domainEmailsList.toString());

            // Save the data to the database
            saveDomainEmails(domainEmailsList);
            return ResponseEntity.ok("File uploaded and data processed successfully.");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Failed to process file: " + e.getMessage());
        }
    }

    private void saveDomainEmails(List<DomainEmails> domainEmailsList) {
        try {
            // Remove existing data and insert new one
            senderEmailsMongoTemplate.dropCollection("SenderEmails");  // Drop the collection before inserting new data
            senderEmailsMongoTemplate.insert(domainEmailsList, "SenderEmails");
            System.out.println("Emails saved successfully.");
        } catch (Exception e) {
            System.err.println("Error saving emails: " + e.getMessage());
        }
    }



    @GetMapping("/collections/{collectionName}")
    public ResponseEntity<Map<String, Object>> getCollectionData(@PathVariable String collectionName) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Fetch the collection data
            List<User> users = mongoTemplate.findAll(User.class, collectionName);
            List<Map<String, Object>> fetchedData = new ArrayList<>();
            List<String> tableHeaders = new ArrayList<>();

            for (User mongoUser : users) {
                fetchedData.addAll(mongoUser.getData());
                if (tableHeaders.isEmpty() && !mongoUser.getData().isEmpty()) {
                    // Populate headers from the first user's data if not empty
                    tableHeaders.addAll(mongoUser.getData().get(0).keySet());
                }
            }

            // Add headers and data to response
            response.put("tableData", fetchedData);
            response.put("tableHeaders", tableHeaders);

            System.out.println(tableHeaders);
        
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            // Log and return error response
            e.printStackTrace();
            response.put("message", "Failed to retrieve data: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }


    @GetMapping("/fetch-data")
    public ResponseEntity<List<Object>> fetchDataFromColumn(
        @RequestParam String collectionName,
        @RequestParam String columnName) {

    // Fetch all documents from the specified collection
    List<User> users = mongoTemplate.findAll(User.class, collectionName);
    List<Object> columnData = new ArrayList<>();

    // Iterate through the documents and extract the desired column data
    for (User user : users) {
        for (Map<String, Object> dataMap : user.getData()) {
            if (dataMap.containsKey(columnName)) {
                columnData.add(dataMap.get(columnName));
            }
        }
    }
    System.out.println(columnData);
    return ResponseEntity.ok(columnData);
}



    @GetMapping("/collections")
    public Set<String> getCollectionNames() {
        // System.out.println(mongoTemplate.getCollectionNames());
        return mongoTemplate.getCollectionNames().stream().collect(Collectors.toSet());
    }
    

    private List<Map<String, Object>> fetchDataFromDb(String collectionName) {
        // Fetch data from MongoDB
        List<User> users = mongoTemplate.findAll(User.class, collectionName);
        List<Map<String, Object>> fetchedData = new ArrayList<>();

        // Include data without row indices
        for (User mongoUser : users) {
            fetchedData.addAll(mongoUser.getData());
        }
        return fetchedData;
    }


    private void extractTableHeaders(MultipartFile file, List<String> tableHeaders) throws IOException {
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);

            // Extract headers
            Row headerRow = sheet.getRow(0);
            if (headerRow != null) {
                tableHeaders.clear(); // Clear previous headers if any
                for (Cell cell : headerRow) {
                    if (cell.getCellType() == CellType.STRING) {
                        tableHeaders.add(cell.getStringCellValue());
                    }
                }
            }
        }
    }

    private void extractTableData(MultipartFile file, List<Map<String, Object>> tableData) throws IOException {
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            DecimalFormat decimalFormat = new DecimalFormat("#.##");

            // Extract rows
            for (int i = 1; i <= sheet.getLastRowNum(); i++) { // Start from the second row (index 1)
                Row row = sheet.getRow(i);
                if (row != null) {
                    Map<String, Object> rowData = new LinkedHashMap<>();
                    for (int j = 0; j < tableHeaders.size(); j++) {
                        Cell cell = row.getCell(j);
                        rowData.put(tableHeaders.get(j), getCellValue(cell, evaluator, decimalFormat));
                    }
                    tableData.add(rowData);
                }
            }
        }
    }

    private Object getCellValue(Cell cell, FormulaEvaluator evaluator, DecimalFormat decimalFormat) {
        if (cell != null) {
            switch (cell.getCellType()) {
                case STRING:
                    return cell.getStringCellValue();
                case NUMERIC:
                    if (DateUtil.isCellDateFormatted(cell)) {
                        return cell.getDateCellValue().toString();
                    } else {
                        return decimalFormat.format(cell.getNumericCellValue());
                    }
                case BOOLEAN:
                    return cell.getBooleanCellValue();
                case FORMULA:
                    CellValue cellValue = evaluator.evaluate(cell);
                    switch (cellValue.getCellType()) {
                        case STRING:
                            return cellValue.getStringValue();
                        case NUMERIC:
                            return decimalFormat.format(cellValue.getNumberValue());
                        case BOOLEAN:
                            return cellValue.getBooleanValue();
                        default:
                            return "Unknown Formula Result";
                    }
                default:
                    return "Unknown Cell Type";
            }
        } else {
            return ""; // Handle missing cells
        }
    }


    @SuppressWarnings("deprecation")
    @PostMapping("/send-email")
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
                    ? extractEmailsFromRoundRobinEmailList(emailDetailsObj)
                    : List.of(senderEmail);
            List<EmailConfig> emailConfigs = ((smtpServer == null || smtpServer.isEmpty()) || (port == null || port.isEmpty()) || (username == null || username.isEmpty()) || (password == null || password.isEmpty()))
                    ? extractEmailConfigs(emailDetailsObj)
                    : List.of(new EmailConfig(smtpServer, port, username,authenticationRequired));

            // System.out.println("senderEmails" + senderEmails);

            // System.out.println("emailConfigs" + emailConfigs);

            // Convert send interval to milliseconds
            long intervalMillis = convertToMilliseconds(sendInterval, timeUnit);

            // Schedule the email sending tasks
            long delayMillis = 0;
            for (int i = 0; i < toEmails.size(); i++) {
                String recipientEmail = toEmails.get(i);
                String currentSenderEmail = senderEmails.get(i % senderEmails.size()); // Round-robin sender emails
                EmailConfig emailConfig = emailConfigs.get(i % emailConfigs.size()); // Round-robin email config

                // System.out.println(recipientEmail);
                // System.out.println(currentSenderEmail);
                // System.out.println("username : " + emailConfig.getUsername() + " pass :" + emailConfig.getPassword());
                // System.out.println(emailConfig.getPassword());
                
                // Log configuration for debugging
                // System.out.println("Sending email with config: " + emailConfig.toString());

                // scheduler.schedule(() -> {
                //     try {
                //         // Send the email using the round-robin sender email and configuration
                //         EmailSender.sendEmail(
                //             distributionList, 
                //             segmentList, 
                //             currentSenderEmail,  // Pass the current sender email as a list
                //             senderName, 
                //             sendInterval, 
                //             timeUnit, 
                //             emailConfig.getSmtpServer(),
                //             emailConfig.isAuthenticationRequired(),
                //             // emailConfig.getUsername(),
                //             currentSenderEmail,
                //             emailConfig.getPassword(),
                //             emailConfig.getPort(),
                //             ssl, 
                //             sendMode, 
                //             List.of(recipientEmail), 
                //             subject, 
                //             messageText, 
                //             plainTextMessage,
                //             sentEmails, 
                //             failedEmails
                //         );

                //          // Log success
                //         // sentEmails.add("Sent message successfully to " + recipientEmail);

                //     } catch (Exception e) {
                //        // Log failure
                //         // failedEmails.add("Failed to send message to " + recipientEmail + " Because " + e.getMessage()); 
                //     }
                // }, new Date(System.currentTimeMillis() + delayMillis));
                // delayMillis += intervalMillis;
            }
            System.out.println(sentEmails);
            System.out.println(failedEmails);

            // Prepare the result to be sent back to the frontend
            Map<String, List<String>> result = new HashMap<>();
            result.put("sentEmails", sentEmails);
            result.put("failedEmails", failedEmails);

            // return ResponseEntity.ok("Emails scheduled successfully!");
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

    private List<String> extractEmailsFromRoundRobinEmailList(List<EmailDetails> emailDetails){
        List<String> emails = new ArrayList<>();
        for(int i = 0;i<emailDetails.size();i++){
            emails.add(emailDetails.get(i).getEmail());
        }
        return emails;
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


































    // @PostMapping("/send-email")
    // public ResponseEntity<String> sendEmail(@RequestBody JsonNode requestData) {
    //     try {
    //         // Extract data from request
    //         String distributionList = requestData.get("distributionList").asText();
    //         String segmentList = requestData.get("segmentList").asText();
    //         String senderEmail = requestData.get("senderEmail").asText();
    //         String senderName = requestData.get("senderName").asText();
    //         int sendInterval = requestData.has("sendInterval") ? requestData.get("sendInterval").asInt() : 0;
    //         String timeUnit = requestData.get("timeUnit").asText();
    //         String smtpServer = requestData.get("smtpServer").asText();
    //         boolean authenticationRequired = requestData.get("authenticationRequired").asBoolean();
    //         String username = requestData.get("username").asText();
    //         String password = requestData.get("password").asText();
    //         String port = requestData.get("port").asText();
    //         boolean ssl = requestData.get("ssl").asBoolean();
    //         String sendMode = requestData.get("sendMode").asText();
    //         String subject = requestData.get("subject").asText();
    //         String messageText = requestData.get("message").asText(); 
    //         String plainTextMessage = requestData.get("plainTextMessage").asText();
            
    //         // Extract the 'to' array
    //         ArrayNode toArray = (ArrayNode) requestData.get("to");
    //         List<String> toEmails = new ArrayList<>();
    //         if (toArray != null) {
    //             for (JsonNode node : toArray) {
    //                 toEmails.add(node.asText());
    //             }
    //         }

    //         // Convert send interval to milliseconds
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

    //         // Schedule the email sending tasks
    //         long delayMillis = 0;
    //         for (String email : toEmails) {
    //             final String recipientEmail = email;
    //             scheduler.schedule(() -> {
    //                 try {
    //                     EmailSender.sendEmail(distributionList, segmentList, senderEmail, senderName, sendInterval, timeUnit, smtpServer, authenticationRequired, username, password, port, ssl, sendMode, List.of(recipientEmail), subject, messageText, plainTextMessage);
    //                 } catch (Exception e) {
    //                     e.printStackTrace();
    //                 }
    //             }, new Date(System.currentTimeMillis() + delayMillis));
    //             delayMillis += intervalMillis;
    //         }

    //         return ResponseEntity.ok("Emails scheduled successfully!");
    //     } catch (Exception e) {
    //         e.printStackTrace();
    //         return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to schedule emails.");
    //     }
    // }









    

