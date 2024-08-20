// File: user-service/src/main/java/com/stillfresh/app/userservice/controller/UserController.java
package com.stillfresh.app.userservice.controller;

import com.stillfresh.app.userservice.dto.PasswordChangeRequest;
import com.stillfresh.app.userservice.model.User;
import com.stillfresh.app.userservice.security.CustomUserDetails;
import com.stillfresh.app.userservice.service.LoginAttemptService;
import com.stillfresh.app.userservice.service.UserService;

import jakarta.validation.Valid;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
public class UserController {

    @Autowired
    private UserService userService;
    
    @Autowired
    private LoginAttemptService loginAttemptService;

    @PostMapping("/register")
    public ResponseEntity<String> registerUser(@Valid @RequestBody User user) {
        userService.saveUser(user);
        return ResponseEntity.ok("User registered successfully");
    }

    @PostMapping("/login")
    public ResponseEntity<String> loginUser(@Valid @RequestBody User user) {
        // Check if the login attempt is within the allowed rate limit
        if (!loginAttemptService.tryLogin()) {
            return ResponseEntity.status(429).body("Too many login attempts, please try again later.");
        }
        
        // If allowed, proceed with authentication (add your authentication logic here)
        // For demonstration purposes, we'll assume the login is always successful
        return ResponseEntity.ok("User logged in successfully");
    }
    
    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> adminEndpoint() {
        return ResponseEntity.ok("Admin content");
    }

    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> users = userService.findAllUsers();
        return ResponseEntity.ok(users);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        User user = userService.findUserById(id);
        return ResponseEntity.ok(user);
    }
    
    @PutMapping("/profile")
    public ResponseEntity<User> updateUserProfile(@RequestBody User updatedUserDetails) {
    	CustomUserDetails customUserDetails = (CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    	User currentUser = customUserDetails.getUser(); // Retrieve the User object


        // Update fields directly on the currentUser
        currentUser.setUsername(updatedUserDetails.getUsername());
        currentUser.setEmail(updatedUserDetails.getEmail());
        // Other fields...

        User updatedUser = userService.saveUser(currentUser);
        return ResponseEntity.ok(updatedUser);
    }
    
    @PutMapping("/change-password")
    public ResponseEntity<String> changeUserPassword(@RequestBody PasswordChangeRequest passwordChangeRequest) {
        // Obtain the authenticated user's details from the security context
    	CustomUserDetails customUserDetails = (CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    	User currentUser = customUserDetails.getUser(); // Retrieve the User object

        // Change the password using the provided new password
        userService.changeUserPassword(currentUser, passwordChangeRequest.getNewPassword());

        return ResponseEntity.ok("Password changed successfully");
    }    

}
