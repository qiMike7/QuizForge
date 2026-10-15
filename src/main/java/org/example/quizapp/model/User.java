package org.example.quizapp.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

// 这个注解会自动在 MongoDB 中创建一个名为 "users" 的集合（表）
@Document(collection = "users")
public class User {
    @Id
    private String id;
    private String username;
    private String password;

    // --- Getter 和 Setter ---
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}