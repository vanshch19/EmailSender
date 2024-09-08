package com.example.EmailSender1.Configuration;

import com.mongodb.client.MongoClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;

@Configuration
public class MongoConfig {

    @Bean
    public MongoTemplate mongoTemplate() {
        // Configure MongoTemplate for 'files' database
        return new MongoTemplate(new SimpleMongoClientDatabaseFactory(
            MongoClients.create("mongodb+srv://vanshch19:vansh@cluster0.0xj70ch.mongodb.net/files?retryWrites=true&w=majority"),
            "files"
        ));
    }

    @Bean
    public MongoTemplate senderEmailsMongoTemplate() {
        // Configure MongoTemplate for 'senderEmails' database
        return new MongoTemplate(new SimpleMongoClientDatabaseFactory(
            MongoClients.create("mongodb+srv://vanshch19:vansh@cluster0.0xj70ch.mongodb.net/senderEmails?retryWrites=true&w=majority"),
            "senderEmails"
        ));
    }
}
