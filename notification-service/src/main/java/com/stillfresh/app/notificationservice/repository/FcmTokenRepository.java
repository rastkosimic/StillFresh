package com.stillfresh.app.notificationservice.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.stillfresh.app.notificationservice.model.FcmTokenEntity;

public interface FcmTokenRepository extends JpaRepository<FcmTokenEntity, UUID> {
	@Query("SELECT f.token FROM FcmTokenEntity f WHERE f.userId = :userId")
	String findTokenByUserId(@Param("userId") String userId); 
	Optional<FcmTokenEntity> findByUserId(String userId);

}

