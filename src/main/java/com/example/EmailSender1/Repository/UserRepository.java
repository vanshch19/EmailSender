package com.example.EmailSender1.Repository;

import java.util.List;
import java.util.Map;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.example.EmailSender1.Model.User;

public interface UserRepository extends MongoRepository<User, Integer>{

    User save(List<Map<String, Object>> tableData);
    
}
