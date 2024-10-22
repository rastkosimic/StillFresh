package com.stillfresh.app.authorizationservice.repository;

import com.stillfresh.app.authorizationservice.model.PasswordResetToken;
import com.stillfresh.app.authorizationservice.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByToken(String token);
    Optional<PasswordResetToken> findByUser(User user);
}
