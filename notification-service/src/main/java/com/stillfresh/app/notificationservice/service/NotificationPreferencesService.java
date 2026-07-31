package com.stillfresh.app.notificationservice.service;

import com.stillfresh.app.notificationservice.model.NotificationPreferencesEntity;
import com.stillfresh.app.notificationservice.repository.NotificationPreferencesRepository;
import com.stillfresh.app.sharedentities.enums.NotificationType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Service
public class NotificationPreferencesService {
    
    @Autowired
    private NotificationPreferencesRepository preferencesRepository;
    
    public NotificationPreferencesEntity getPreferences(String userId) {
        return preferencesRepository.findByUserId(userId)
                .orElse(null);
    }
    
    public NotificationPreferencesEntity getOrCreatePreferences(String userId) {
        return preferencesRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultPreferences(userId));
    }
    
    public NotificationPreferencesEntity updatePreferences(String userId, 
                                                         Set<NotificationType> enabledTypes,
                                                         boolean pushEnabled,
                                                         boolean emailEnabled,
                                                         boolean smsEnabled) {
        Optional<NotificationPreferencesEntity> existing = preferencesRepository.findByUserId(userId);
        NotificationPreferencesEntity preferences = existing.orElse(new NotificationPreferencesEntity());
        
        preferences.setUserId(userId);
        preferences.setEnabledTypes(enabledTypes);
        preferences.setPushEnabled(pushEnabled);
        preferences.setEmailEnabled(emailEnabled);
        preferences.setSmsEnabled(smsEnabled);
        
        return preferencesRepository.save(preferences);
    }
    
    public boolean isNotificationTypeEnabled(String userId, NotificationType type) {
        NotificationPreferencesEntity preferences = getOrCreatePreferences(userId);
        return preferences.getEnabledTypes() != null && 
               preferences.getEnabledTypes().contains(type);
    }
    
    public boolean isPushEnabled(String userId) {
        NotificationPreferencesEntity preferences = getOrCreatePreferences(userId);
        return preferences.isPushEnabled();
    }
    
    private NotificationPreferencesEntity createDefaultPreferences(String userId) {
        NotificationPreferencesEntity preferences = new NotificationPreferencesEntity();
        preferences.setUserId(userId);
        preferences.setPushEnabled(true);
        preferences.setEmailEnabled(false);
        preferences.setSmsEnabled(false);
        
        // Enable all notification types by default
        Set<NotificationType> allTypes = new HashSet<>();
        for (NotificationType type : NotificationType.values()) {
            allTypes.add(type);
        }
        preferences.setEnabledTypes(allTypes);
        
        return preferencesRepository.save(preferences);
    }
}








