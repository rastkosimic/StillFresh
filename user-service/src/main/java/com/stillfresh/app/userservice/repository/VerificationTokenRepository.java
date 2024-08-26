package com.stillfresh.app.userservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.stillfresh.app.userservice.model.VerificationToken;

public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Long> {
    VerificationToken findByToken(String token);
}
