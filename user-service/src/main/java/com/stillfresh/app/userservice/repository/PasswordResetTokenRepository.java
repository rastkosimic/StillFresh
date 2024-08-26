package com.stillfresh.app.userservice.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.stillfresh.app.userservice.model.PasswordResetToken;
import com.stillfresh.app.userservice.model.User;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    PasswordResetToken findByToken(String token);
    
    Optional<PasswordResetToken> findByUser(User user);
    
    void deleteByUser(User user);

}
