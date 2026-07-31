package com.stillfresh.app.notificationservice.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.stillfresh.app.notificationservice.model.FcmTokenEntity;

public interface FcmTokenRepository extends JpaRepository<FcmTokenEntity, UUID> {
	@Query("SELECT f.token FROM FcmTokenEntity f WHERE f.userId = :userId")
	String findTokenByUserId(@Param("userId") String userId); 
	
	Optional<FcmTokenEntity> findByUserId(String userId);
	
	@Modifying
	@Transactional
	@Query("UPDATE FcmTokenEntity f SET f.token = :token WHERE f.userId = :userId")
	int updateTokenByUserId(@Param("userId") String userId, @Param("token") String token);
}

