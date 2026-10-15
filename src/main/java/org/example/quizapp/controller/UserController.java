package org.example.quizapp.controller;

import org.example.quizapp.model.User;
import org.example.quizapp.service.UserService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public String register(@RequestParam String username, @RequestParam String password) {
        try {
            userService.register(username, password);
            return "success: 注册成功";
        } catch (IllegalArgumentException e) {
            return "error: " + e.getMessage();
        }
    }

    @PostMapping("/login")
    public String login(@RequestParam String username, @RequestParam String password) {
        try {
            User user = userService.login(username, password);
            return "success:" + user.getId();
        } catch (IllegalArgumentException e) {
            return "error: " + e.getMessage();
        }
    }
}
