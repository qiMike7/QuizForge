package org.example.quizapp.repository;

import org.example.quizapp.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends MongoRepository<User, String> {
    // 按照 Spring Data 的命名规范，写下这行代码，系统就会自动帮你实现“根据用户名查询用户”的功能
    User findByUsername(String username);
}