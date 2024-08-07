// File: user-service/src/main/java/com/stillfresh/app/userservice/controller/UserController.java
package com.stillfresh.app.userservice.controller;

import com.stillfresh.app.userservice.model.User;
import com.stillfresh.app.userservice.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public ResponseEntity<String> registerUser(@RequestBody User user) {
        userService.saveUser(user);
        return ResponseEntity.ok("User registered successfully");
    }

    @PostMapping("/login")
    public ResponseEntity<String> loginUser(@RequestBody User user) {
        // Add login logic here
        return ResponseEntity.ok("User logged in successfully");
    }
}
