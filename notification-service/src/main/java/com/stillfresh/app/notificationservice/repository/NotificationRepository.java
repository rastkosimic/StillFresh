package com.stillfresh.app.notificationservice.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.stillfresh.app.notificationservice.model.NotificationEntity;
import com.stillfresh.app.notificationservice.model.NotificationStatus;

@Repository
public interface NotificationRepository extends JpaRepository<NotificationEntity, UUID> {
    
    List<NotificationEntity> findByUserIdOrderByCreatedAtDesc(String userId);
    
    List<NotificationEntity> findByStatus(NotificationStatus status);
    
    List<NotificationEntity> findByUserIdAndTypeOrderByCreatedAtDesc(String userId, String type);
} 