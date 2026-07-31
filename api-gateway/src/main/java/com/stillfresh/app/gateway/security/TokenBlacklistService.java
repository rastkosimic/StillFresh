package com.stillfresh.app.gateway.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class TokenBlacklistService {

    private static final String TOKEN_ID_BLACKLIST_PREFIX = "blacklisted_jti_";

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    public boolean isTokenIdBlacklisted(String tokenId) {
        if (tokenId == null || tokenId.isEmpty()) {
            return false;
        }
        Object raw = redisTemplate.opsForValue().get(TOKEN_ID_BLACKLIST_PREFIX + tokenId);
        if (raw == null) {
            return false;
        }
        // RedisTemplate is configured with Object values, so the stored type might be Boolean,
        // String ("true"/"false"), or numeric. Handle all without throwing.
        if (raw instanceof Boolean b) {
            return Boolean.TRUE.equals(b);
        }
        if (raw instanceof String s) {
            return Boolean.parseBoolean(s);
        }
        if (raw instanceof Number n) {
            return n.intValue() == 1;
        }
        return false;
    }
}

