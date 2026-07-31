package com.stillfresh.app.notificationservice.service;

import com.stillfresh.app.notificationservice.config.NotificationMetrics;
import com.stillfresh.app.notificationservice.dto.ApiResponse;
import com.stillfresh.app.notificationservice.model.FcmTokenEntity;
import com.stillfresh.app.notificationservice.repository.FcmTokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class FcmTokenService {

    @Autowired
    private FcmTokenRepository fcmTokenRepository;
    
    @Autowired
    private NotificationMetrics metrics;

    @Transactional
    public ResponseEntity<ApiResponse<FcmTokenEntity>> registerToken(String userId, String token) {
        try {
            // First try to update existing token
            int updatedRows = fcmTokenRepository.updateTokenByUserId(userId, token);
            
            if (updatedRows > 0) {
                // Token was updated successfully
                Optional<FcmTokenEntity> updatedEntity = fcmTokenRepository.findByUserId(userId);
                if (updatedEntity.isPresent()) {
                    metrics.fcmTokenRegisteredCounter().increment();
                    return ResponseEntity.ok(ApiResponse.success("FCM token updated successfully", updatedEntity.get()));
                }
            }
            
            // No existing token found, create new one
            FcmTokenEntity newEntity = new FcmTokenEntity();
            newEntity.setUserId(userId);
            newEntity.setToken(token);
            FcmTokenEntity savedEntity = fcmTokenRepository.save(newEntity);
            
            // Increment metrics
            metrics.fcmTokenRegisteredCounter().increment();
            
            return ResponseEntity.ok(ApiResponse.success("FCM token registered successfully", savedEntity));
            
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            // Handle race condition where token was created between update and insert
            try {
                return updateExistingToken(userId, token);
            } catch (Exception retryException) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Failed to register FCM token after retry", retryException.getMessage()));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to register FCM token", e.getMessage()));
        }
    }
    
    @Transactional
    private ResponseEntity<ApiResponse<FcmTokenEntity>> updateExistingToken(String userId, String token) {
        Optional<FcmTokenEntity> existing = fcmTokenRepository.findByUserId(userId);
        if (existing.isPresent()) {
            FcmTokenEntity entity = existing.get();
            entity.setToken(token);
            FcmTokenEntity savedEntity = fcmTokenRepository.save(entity);
            metrics.fcmTokenRegisteredCounter().increment();
            return ResponseEntity.ok(ApiResponse.success("FCM token updated successfully", savedEntity));
        } else {
            return ResponseEntity.badRequest().body(ApiResponse.error("FCM token not found for update"));
        }
    }

    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<FcmTokenEntity>> getToken(String userId) {
        try {
            Optional<FcmTokenEntity> tokenEntity = fcmTokenRepository.findByUserId(userId);
            if (tokenEntity.isPresent()) {
                return ResponseEntity.ok(ApiResponse.success("FCM token retrieved successfully", tokenEntity.get()));
            } else {
                return ResponseEntity.ok(ApiResponse.error("FCM token not found"));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to retrieve FCM token", e.getMessage()));
        }
    }

    @Transactional
    public ResponseEntity<ApiResponse<Void>> deleteToken(String userId) {
        try {
            Optional<FcmTokenEntity> tokenEntity = fcmTokenRepository.findByUserId(userId);
            if (tokenEntity.isPresent()) {
                fcmTokenRepository.delete(tokenEntity.get());
                return ResponseEntity.ok(ApiResponse.success("FCM token deleted successfully"));
            } else {
                return ResponseEntity.ok(ApiResponse.error("FCM token not found"));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to delete FCM token", e.getMessage()));
        }
    }
}
