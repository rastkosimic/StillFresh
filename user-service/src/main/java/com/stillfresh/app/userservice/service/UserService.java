package com.stillfresh.app.userservice.service;

import com.stillfresh.app.userservice.exception.ResourceNotFoundException;
import com.stillfresh.app.userservice.model.User;
import com.stillfresh.app.userservice.model.User.Role;
import com.stillfresh.app.userservice.repository.UserRepository;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;


    public User saveUser(User user) {
        // Check if username or email already exists
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new IllegalArgumentException("Username is already taken");
        }

        if (userRepository.existsByEmail(user.getEmail())) {
            throw new IllegalArgumentException("Email is already registered");
        }
        
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRole(Role.USER);  // Default role
        return userRepository.save(user);
    }
    
    public void saveAdminUser(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRole(Role.ADMIN);  // Assign ADMIN role
        userRepository.save(user);
    }
    
    public void assignAdminRole(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setRole(Role.ADMIN);
        userRepository.save(user);
    }
    
    public List<User> findAllUsers() {
        return userRepository.findAll();
    }

    public User findUserById(Long id) {
        Optional<User> user = userRepository.findById(id);
        return user.orElseThrow(() -> new RuntimeException("User not found"));
    }
    
    // Update user profile
    public User updateUserProfile(Long userId, User updatedUser) {
        Optional<User> existingUserOptional = userRepository.findById(userId);
        if (existingUserOptional.isPresent()) {
            User existingUser = existingUserOptional.get();
            existingUser.setUsername(updatedUser.getUsername());
            existingUser.setEmail(updatedUser.getEmail());
            // Add any other fields that can be updated
            return userRepository.save(existingUser);
        } else {
            throw new ResourceNotFoundException("User not found");
        }
    }
    
    // Change user password
    public void changeUserPassword(User user, String newPassword) {
        // Encode the new password
        user.setPassword(passwordEncoder.encode(newPassword));
        // Save the updated user with the new password
        userRepository.save(user);
    }
}
