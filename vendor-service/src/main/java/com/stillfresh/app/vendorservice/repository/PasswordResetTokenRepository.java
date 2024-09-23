package com.stillfresh.app.vendorservice.repository;

import com.stillfresh.app.vendorservice.model.PasswordResetToken;
import com.stillfresh.app.vendorservice.model.Vendor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByToken(String token);
    Optional<PasswordResetToken> findByVendor(Vendor vendor);
}
