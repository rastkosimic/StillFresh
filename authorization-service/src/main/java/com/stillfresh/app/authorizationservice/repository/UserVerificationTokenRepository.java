package com.stillfresh.app.authorizationservice.repository;

import com.stillfresh.app.authorizationservice.model.UserVerificationToken;
import com.stillfresh.app.authorizationservice.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserVerificationTokenRepository extends JpaRepository<UserVerificationToken, Long> {
    Optional<UserVerificationToken> findByToken(String token);
    Optional<UserVerificationToken> findByUser(User user);
}
