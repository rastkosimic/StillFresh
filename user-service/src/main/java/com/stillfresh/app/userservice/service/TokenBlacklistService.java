package com.stillfresh.app.userservice.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class TokenBlacklistService {

    private static final String TOKEN_BLACKLIST_PREFIX = "blacklisted_token_";

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    // Add a token to the Redis blacklist with an expiry time
    public void addTokenToBlacklist(String token, long expiryDurationInMillis) {
        redisTemplate.opsForValue().set(TOKEN_BLACKLIST_PREFIX + token, true, expiryDurationInMillis, TimeUnit.MILLISECONDS);
    }

    // Check if the token is blacklisted (exists in Redis)
    public boolean isTokenBlacklisted(String token) {
        Boolean isBlacklisted = (Boolean) redisTemplate.opsForValue().get(TOKEN_BLACKLIST_PREFIX + token);
        return Boolean.TRUE.equals(isBlacklisted);  // Return true if blacklisted, false otherwise
    }
}
