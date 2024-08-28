package com.example.EmailSender1.Model;

import java.util.List;
import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;


@Document
public class User {

    @Field("data")
    private List<Map<String, Object>> data;

    public User() {}  // Default constructor

    public User(List<Map<String, Object>> data) {
        this.data = data;
    }

    public List<Map<String, Object>> getData() {
        return data;
    }

    public void setData(List<Map<String, Object>> data) {
        this.data = data;
    }
}


// @Document(collection = "users") // Use a default collection name or specify it dynamically
// public class User {
//     @Id
//     private String id;
//     private Map<String, Object> data;

//     // Constructor
//     public User(Map<String, Object> data) {
//         this.data = data;
//     }

//     // Default constructor
//     public User() {
//     }

//     // Getters and setters
//     public String getId() {
//         return id;
//     }

//     public void setId(String id) {
//         this.id = id;
//     }

//     public Map<String, Object> getData() {
//         return data;
//     }

//     public void setData(Map<String, Object> data) {
//         this.data = data;
//     }
// }