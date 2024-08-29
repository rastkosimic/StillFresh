package com.stillfresh.app.userservice.service;

import com.stillfresh.app.userservice.model.TokenBlacklistEntry;
import com.stillfresh.app.userservice.repository.TokenBlacklistRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class TokenBlacklistService {

    @Autowired
    private TokenBlacklistRepository tokenBlacklistRepository;

    public void addTokenToBlacklist(String token) {
        TokenBlacklistEntry entry = new TokenBlacklistEntry(token, new Date());
        tokenBlacklistRepository.save(entry);
    }

    public boolean isTokenBlacklisted(String token) {
        return tokenBlacklistRepository.existsByToken(token);
    }
}
