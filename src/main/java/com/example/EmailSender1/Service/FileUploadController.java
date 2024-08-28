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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.EmailSender1.Model.User;
import com.example.EmailSender1.Repository.UserRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

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

    private List<String> tableHeaders = new ArrayList<>();

    @Autowired
    private MongoTemplate mongoTemplate;

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
    public ResponseEntity<String> sendEmail(@RequestBody JsonNode requestData) {
        try {
            // Extract data from request
            String distributionList = requestData.get("distributionList").asText();
            String segmentList = requestData.get("segmentList").asText();
            String senderEmail = requestData.get("senderEmail").asText();
            String senderName = requestData.get("senderName").asText();
            int sendInterval = requestData.has("sendInterval") ? requestData.get("sendInterval").asInt() : 0;
            String timeUnit = requestData.get("timeUnit").asText();
            String smtpServer = requestData.get("smtpServer").asText();
            boolean authenticationRequired = requestData.get("authenticationRequired").asBoolean();
            String username = requestData.get("username").asText();
            String password = requestData.get("password").asText();
            String port = requestData.get("port").asText();
            boolean ssl = requestData.get("ssl").asBoolean();
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

            // Convert send interval to milliseconds
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

            // Schedule the email sending tasks
            long delayMillis = 0;
            for (String email : toEmails) {
                final String recipientEmail = email;
                scheduler.schedule(() -> {
                    try {
                        EmailSender.sendEmail(distributionList, segmentList, senderEmail, senderName, sendInterval, timeUnit, smtpServer, authenticationRequired, username, password, port, ssl, sendMode, List.of(recipientEmail), subject, messageText, plainTextMessage);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }, new Date(System.currentTimeMillis() + delayMillis));
                delayMillis += intervalMillis;
            }

            return ResponseEntity.ok("Emails scheduled successfully!");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to schedule emails.");
        }
    }









    
//     @PostMapping("/send-email")
//     public ResponseEntity<String> sendEmail(@RequestBody JsonNode requestData) {
//         try {
//             // Extract data from request
//             String distributionList = requestData.get("distributionList").asText();
//             String segmentList = requestData.get("segmentList").asText();
//             String senderEmail = requestData.get("senderEmail").asText();
//             String senderName = requestData.get("senderName").asText();
//             int sendInterval = requestData.has("sendInterval") ? requestData.get("sendInterval").asInt() : 0;
//             String timeUnit = requestData.get("timeUnit").asText();
//             String smtpServer = requestData.get("smtpServer").asText();
//             boolean authenticationRequired = requestData.get("authenticationRequired").asBoolean();
//             String username = requestData.get("username").asText();
//             String password = requestData.get("password").asText();
//             String port = requestData.get("port").asText();
//             boolean ssl = requestData.get("ssl").asBoolean();
//             String sendMode = requestData.get("sendMode").asText();
//             String subject = requestData.get("subject").asText();
//             String messageText = requestData.get("message").asText(); 
//             String plainTextMessage = requestData.get("plainTextMessage").asText();
//             // Extract the 'to' array
//                 ArrayNode toArray = (ArrayNode) requestData.get("to");
//                 List<String> toEmails = new ArrayList<>();
//                 if (toArray != null) {
//                     for (JsonNode node : toArray) {
//                         toEmails.add(node.asText());
//                     }
//                 }

//                 System.out.println(sendInterval);
//                 // // Data loading from local Storage 
//                 // JsonNode composedData = requestData.get("composedData");

//                 // System.out.println();

//                 // Convert send interval to milliseconds
//             long intervalMillis;
//             switch (timeUnit) {
//                 case "seconds":
//                     intervalMillis = sendInterval * 1000;
//                     break;
//                 case "minutes":
//                     intervalMillis = sendInterval * 60000;
//                     break;
//                 case "hours":
//                     intervalMillis = sendInterval * 3600000;
//                     break;
//                 default:
//                     intervalMillis = 0;
//             }

//              // Schedule the email sending task
//             scheduler.scheduleAtFixedRate(() -> {
//                 EmailSender.sendEmail(distributionList, segmentList, senderEmail, senderName, sendInterval, smtpServer, plainTextMessage, authenticationRequired, username, password, port, ssl, sendMode, toEmails, subject, messageText, plainTextMessage);
//             }, 0, intervalMillis, TimeUnit.MILLISECONDS);


//             // EmailSender.sendEmail(distributionList, segmentList, senderEmail, senderName, sendInterval, timeUnit, smtpServer, authenticationRequired, username, password, port, ssl, sendMode, toEmails, subject, messageText, plainTextMessage);

//             return ResponseEntity.ok("Emails sent successfully!");
//         } catch (Exception e) {
//             e.printStackTrace();
//             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to send emails.");
//         }
//     }
}


