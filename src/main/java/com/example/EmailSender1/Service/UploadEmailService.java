// package com.example.EmailSender1.Service;

// import java.io.InputStream;
// import java.util.ArrayList;
// import java.util.HashMap;
// import java.util.LinkedHashMap;
// import java.util.List;
// import java.util.Map;

// import org.apache.poi.ss.usermodel.Cell;
// import org.apache.poi.ss.usermodel.Row;
// import org.apache.poi.ss.usermodel.Sheet;
// import org.apache.poi.ss.usermodel.Workbook;
// import org.apache.poi.xssf.usermodel.XSSFWorkbook;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.data.mongodb.core.MongoTemplate;
// import org.springframework.stereotype.Service;
// import org.springframework.web.multipart.MultipartFile;

// import com.example.EmailSender1.Model.DomainEmails;

// @Service
// public class UploadEmailService {
//     @Autowired
//     private static MongoTemplate mongoTemplate; // MongoTemplate for MongoDB operations

//     private String COLLECTION_NAME = "domainEmails";

//     public List<DomainEmails> processFile(MultipartFile file) throws Exception {
//     try (InputStream is = file.getInputStream()) {
//         Workbook workbook = new XSSFWorkbook(is);
//         Sheet sheet = workbook.getSheetAt(0);

//         Map<String, List<String>> domainEmailsMap = new LinkedHashMap<>();

//         for (Row row : sheet) {
//             if (row.getRowNum() == 0) continue; // Skip header row
//             String domainName = row.getCell(0).getStringCellValue();
//             String email = row.getCell(1).getStringCellValue();

//             domainEmailsMap.computeIfAbsent(domainName, k -> new ArrayList<>()).add(email);
//         }

//         // Convert the map to a list and save to MongoDB
//         List<DomainEmails> domainEmailsList = convertToDomainEmails(domainEmailsMap);

//         // Log or print the data to ensure it's correct
//         for (DomainEmails domainEmails : domainEmailsList) {
//             System.out.println("Domain: " + domainEmails.getDomainName());
//             System.out.println("Emails: " + domainEmails.getEmails());
//         }

//         // saveDomainEmails(domainEmailsList);
//         return domainEmailsList;
//     }
// }

// private List<DomainEmails> convertToDomainEmails(Map<String, List<String>> domainEmailsMap) {
//     List<DomainEmails> domainEmailsList = new ArrayList<>();
//     for (Map.Entry<String, List<String>> entry : domainEmailsMap.entrySet()) {
//         String domain = entry.getKey();
//         List<String> emails = entry.getValue();
//         domainEmailsList.add(new DomainEmails(domain, emails));
//     }
//     return domainEmailsList;
// }

// private void saveDomainEmails(List<DomainEmails> domainEmailsList) {
//     try {
//         // mongoTemplate.dropCollection(COLLECTION_NAME); // Optional: Drop the collection if you want to start fresh
//         DomainEmails domainEmails = new DomainEmails(domainEmailsList);
//         mongoTemplate.insert(domainEmails); // Insert all domainEmails entries
//     } catch (Exception e) {
//         System.err.println("Error saving entries: " + e.getMessage());
//         e.printStackTrace();
//     }
// }

// }






// public static List<DomainEmails> processFile(MultipartFile file) throws Exception {
//         List<DomainEmails> domainEmailsList = new ArrayList<>();
        
//         try (InputStream is = file.getInputStream()) {
//             Workbook workbook = new XSSFWorkbook(is);
//             Sheet sheet = workbook.getSheetAt(0);

//             Map<String, List<String>> domainEmailsMap = new LinkedHashMap<>();
        
//             for (Row row : sheet) {
//                 if (row.getRowNum() == 0) continue; // Skip header row
//                 String domainName = row.getCell(0).getStringCellValue();
//                 String email = row.getCell(1).getStringCellValue();

//                 domainEmailsMap.computeIfAbsent(domainName, k -> new ArrayList<>()).add(email);
//             }

//             // Convert the map to a list of DomainEmails and return
//             for (Map.Entry<String, List<String>> entry : domainEmailsMap.entrySet()) {
//                 domainEmailsList.add(new DomainEmails(entry.getKey(), entry.getValue()));
//             }
//         }

//         // System.out.println(domainEmailsList);
//         return domainEmailsList;
//     }

//     public static void saveDomainEmails(List<DomainEmails> domainEmailsList) {
//         mongoTemplate.dropCollection(COLLECTION_NAME); // Optional: Drop the collection if you want to start fresh
//         DomainEmails domainEmails = new DomainEmails(domainEmailsList);
//         mongoTemplate.insert(domainEmails,COLLECTION_NAME); // Insert all domain emails
//     }