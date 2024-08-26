package com.stillfresh.app.userservice.service;

import com.stillfresh.app.userservice.exception.ResourceNotFoundException;
import com.stillfresh.app.userservice.model.User;
import com.stillfresh.app.userservice.model.User.Role;
import com.stillfresh.app.userservice.repository.UserRepository;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    
    public User registerUser(User user) {
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new IllegalArgumentException("Username is already taken");
        }

        if (userRepository.existsByEmail(user.getEmail())) {
            throw new IllegalArgumentException("Email is already registered");
        }
        
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRole(Role.USER);  // Default role
        
        logger.info("Registering user with username: {}" + user.getUsername());
        
        return userRepository.save(user);
    }

    public User updateUser(User user) {
        logger.info("Updating user with username: {}" + user.getUsername());
        return userRepository.save(user);
    }
    
    public void saveAdminUser(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRole(Role.ADMIN);  // Assign ADMIN role
        
        logger.info("Registering ADMIN with username: {}", user.getUsername());
        
        userRepository.save(user);
    }
    
    public void assignAdminRole(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setRole(Role.ADMIN);
        
        logger.info("ADMIN role assigned to the user with username: {}", user.getUsername());
        
        userRepository.save(user);
    }
    
    public List<User> findAllUsers() {
    	logger.info("Finding all users");
        return userRepository.findAll();
    }

    public User findUserById(Long id) {
        Optional<User> user = userRepository.findById(id);
        logger.info("Finding a user {}", user.get().getUsername(),", with id: {}",id);
        return user.orElseThrow(() -> new RuntimeException("User not found"));
    }
    
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }
    
    // Update user profile
    public User updateUserProfile(Long userId, User updatedUser) {
        Optional<User> existingUserOptional = userRepository.findById(userId);
        if (existingUserOptional.isPresent()) {
            User existingUser = existingUserOptional.get();
            existingUser.setUsername(updatedUser.getUsername());
            existingUser.setEmail(updatedUser.getEmail());
            // Add any other fields that can be updated
            
            logger.info("Updated details for the user with id: {}", existingUser.getId());
            return userRepository.save(existingUser);
        } else {
            throw new ResourceNotFoundException("User not found");
        }
    }
    
    // Change user password
    public void changeUserPassword(User user, String newPassword) {
        // Encode the new password
    	logger.info("USERNAME: {}", user.getUsername());
    	logger.info("PASSWORD: {}", user.getPassword());
    	logger.info("EMAIL: {}", user.getEmail());
    	logger.info("ID: {}", user.getId());
    	logger.info("ACTIVE: {}", user.isActive());
        user.setPassword(passwordEncoder.encode(newPassword));
        logger.info("Password changing for the user with username: {}", user.getUsername());
        // Save the updated user with the new password
        userRepository.save(user);
    }
}
