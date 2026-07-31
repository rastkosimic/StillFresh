package com.stillfresh.app.notificationservice.model;

import com.stillfresh.app.sharedentities.enums.NotificationType;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.Set;

@Entity
@Table(name = "notification_preferences")
public class NotificationPreferencesEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String userId;
    
    @ElementCollection
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "enabled_notification_types", joinColumns = @JoinColumn(name = "preferences_id"))
    @Column(name = "notification_type")
    private Set<NotificationType> enabledTypes;
    
    @Column(nullable = false)
    private boolean pushEnabled = true;
    
    @Column(nullable = false)
    private boolean emailEnabled = false;
    
    @Column(nullable = false)
    private boolean smsEnabled = false;
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;
    
    @UpdateTimestamp
    @Column
    private OffsetDateTime updatedAt;
    
    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Set<NotificationType> getEnabledTypes() {
        return enabledTypes;
    }

    public void setEnabledTypes(Set<NotificationType> enabledTypes) {
        this.enabledTypes = enabledTypes;
    }

    public boolean isPushEnabled() {
        return pushEnabled;
    }

    public void setPushEnabled(boolean pushEnabled) {
        this.pushEnabled = pushEnabled;
    }

    public boolean isEmailEnabled() {
        return emailEnabled;
    }

    public void setEmailEnabled(boolean emailEnabled) {
        this.emailEnabled = emailEnabled;
    }

    public boolean isSmsEnabled() {
        return smsEnabled;
    }

    public void setSmsEnabled(boolean smsEnabled) {
        this.smsEnabled = smsEnabled;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}








