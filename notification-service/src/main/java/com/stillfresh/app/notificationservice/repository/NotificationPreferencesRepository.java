package com.stillfresh.app.notificationservice.repository;

import com.stillfresh.app.notificationservice.model.NotificationPreferencesEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NotificationPreferencesRepository extends JpaRepository<NotificationPreferencesEntity, Long> {
    Optional<NotificationPreferencesEntity> findByUserId(String userId);
}








