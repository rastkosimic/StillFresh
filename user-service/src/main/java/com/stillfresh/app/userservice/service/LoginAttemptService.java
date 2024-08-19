package com.stillfresh.app.userservice.service;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.Refill;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class LoginAttemptService {

    private final Bucket bucket;

    public LoginAttemptService() {
        // Configure the rate limiting to allow 5 login attempts per minute
        Bandwidth limit = Bandwidth.classic(5, Refill.greedy(5, Duration.ofMinutes(1)));
        this.bucket = Bucket4j.builder().addLimit(limit).build();
    }

    // Method to check if a login attempt is allowed and provide a more detailed response
    public boolean tryLogin() {
        if (bucket.tryConsume(1)) {
            return true;
        } else {
            // Log the rate limit violation, send notifications, etc.
            System.out.println("Too many login attempts");
            return false;
        }
    }
}
