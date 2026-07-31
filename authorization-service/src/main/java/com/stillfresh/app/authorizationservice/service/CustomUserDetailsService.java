package com.stillfresh.app.authorizationservice.service;

import com.stillfresh.app.authorizationservice.model.User;
import com.stillfresh.app.authorizationservice.repository.UserRepository;
import com.stillfresh.app.authorizationservice.security.CustomUserDetails;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    private static final Logger logger = LoggerFactory.getLogger(CustomUserDetailsService.class);
    
    @Override
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        // Single-query lookup: email or username, case-insensitive (at most one DB round-trip)
        User user = userRepository.findByEmailOrUsernameIgnoreCase(identifier).orElse(null);

        if (user == null) {
            logger.warn("User not found for identifier: {}", identifier);
            throw new UsernameNotFoundException("User not found: " + identifier);
        }

        logger.debug("Loaded user: ID={}, Role={}, Status={}", user.getId(), user.getRole(), user.getStatus());
        return new CustomUserDetails(user);
    }
    
    public UserDetails loadUserByEmail(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        return new CustomUserDetails(user);
    }

    public UserDetails loadUserByEmailOrUsername(String identifier) throws UsernameNotFoundException {
        User user;

        if (isEmail(identifier)) {
            user = userRepository.findByEmail(identifier)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + identifier));
        } else {
            user = userRepository.findByUsername(identifier)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + identifier));
        }

        return new CustomUserDetails(user);
    }

    private boolean isEmail(String identifier) {
        // Basic check to determine if the identifier is an email address
        return identifier.contains("@");
    }
}
