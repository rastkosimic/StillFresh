package com.stillfresh.app.userservice.service;

import com.stillfresh.app.userservice.model.User;
import com.stillfresh.app.userservice.repository.UserRepository;
import com.stillfresh.app.userservice.security.CustomUserDetails;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    private static final Logger logger = LoggerFactory.getLogger(CustomUserDetailsService.class);

    // In-memory cache to avoid a DB round-trip on every authenticated request.
    // GatewayTrustFilter calls loadUserByUsername() for every request, so without
    // this cache every API call (favorites, orders, etc.) pays a DB lookup cost.
    private static final long CACHE_TTL_MS = 5 * 60 * 1000L; // 5 minutes
    private final ConcurrentHashMap<String, CachedEntry> userDetailsCache = new ConcurrentHashMap<>();

    private static final class CachedEntry {
        final UserDetails details;
        final long expiresAt;
        CachedEntry(UserDetails details) {
            this.details = details;
            this.expiresAt = System.currentTimeMillis() + CACHE_TTL_MS;
        }
    }

    @Override
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        CachedEntry cached = userDetailsCache.get(identifier);
        if (cached != null && System.currentTimeMillis() < cached.expiresAt) {
            return cached.details;
        }

        User user;
        if (isEmail(identifier)) {
            user = userRepository.findByEmail(identifier)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + identifier));
        } else {
            user = userRepository.findByUsername(identifier)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + identifier));
        }

        // Intentionally avoid logging password hashes for security and log noise reasons
        logger.debug("Loaded user details for identifier: {}", identifier);
        UserDetails details = new CustomUserDetails(user);
        userDetailsCache.put(identifier, new CachedEntry(details));
        return details;
    }

    public void evictUserCache(User user) {
        logger.info("Evicting cache for user: {}", user.getUsername());
        userDetailsCache.remove(user.getUsername());
        if (user.getEmail() != null) {
            userDetailsCache.remove(user.getEmail());
        }
    }

    private boolean isEmail(String identifier) {
        // Basic regex to check if the identifier is an email
        return identifier.contains("@");
    }
}
