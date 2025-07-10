package com.stillfresh.app.notificationservice.service;

import com.stillfresh.app.notificationservice.model.FcmTokenEntity;
import com.stillfresh.app.notificationservice.repository.FcmTokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class FcmTokenService {

    @Autowired
    private FcmTokenRepository fcmTokenRepository;

    public ResponseEntity<String> registerToken(String userId, String token) {
        Optional<FcmTokenEntity> existing = fcmTokenRepository.findByUserId(userId);
        FcmTokenEntity entity = existing.orElse(new FcmTokenEntity());
        entity.setUserId(userId);
        entity.setToken(token);
        fcmTokenRepository.save(entity);
        return ResponseEntity.ok("FCM token registered successfully.");
    }

    public ResponseEntity<?> getToken(String userId) {
        return fcmTokenRepository.findByUserId(userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    public ResponseEntity<String> deleteToken(String userId) {
        fcmTokenRepository.findByUserId(userId).ifPresent(fcmTokenRepository::delete);
        return ResponseEntity.ok("FCM token deleted.");
    }
}
