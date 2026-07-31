package com.stillfresh.app.notificationservice.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.stillfresh.app.notificationservice.model.NotificationEntity;
import com.stillfresh.app.notificationservice.model.NotificationStatus;

@Repository
public interface NotificationRepository extends JpaRepository<NotificationEntity, UUID> {
    
    List<NotificationEntity> findByUserIdOrderByCreatedAtDesc(String userId);
    
    /** Notifications for user that are not soft-deleted (for inbox listing). */
    List<NotificationEntity> findByUserIdAndDeletedFalseOrderByCreatedAtDesc(String userId);
    
    List<NotificationEntity> findByStatus(NotificationStatus status);
    
    List<NotificationEntity> findByUserIdAndStatus(String userId, NotificationStatus status);
    
    List<NotificationEntity> findByUserIdAndTypeOrderByCreatedAtDesc(String userId, String type);
    
    List<NotificationEntity> findByUserIdAndIsReadOrderByCreatedAtDesc(String userId, boolean isRead);
    
    /** Unread notifications for user that are not soft-deleted. */
    List<NotificationEntity> findByUserIdAndIsReadAndDeletedFalseOrderByCreatedAtDesc(String userId, boolean isRead);
    
    List<NotificationEntity> findByUserIdAndStatusAndIsReadOrderByCreatedAtDesc(String userId, NotificationStatus status, boolean isRead);
    
    /** Retention: hard-delete notifications older than the given cutoff. */
    @Modifying
    @Query("DELETE FROM NotificationEntity n WHERE n.createdAt < :cutoff")
    int deleteByCreatedAtBefore(@Param("cutoff") OffsetDateTime cutoff);
} 