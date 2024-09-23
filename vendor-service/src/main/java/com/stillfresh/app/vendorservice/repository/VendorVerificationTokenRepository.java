package com.stillfresh.app.vendorservice.repository;

import com.stillfresh.app.vendorservice.model.VendorVerificationToken;
import com.stillfresh.app.vendorservice.model.Vendor;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface VendorVerificationTokenRepository extends JpaRepository<VendorVerificationToken, Long> {
    Optional<VendorVerificationToken> findByToken(String token);
    Optional<VendorVerificationToken> findByVendor(Vendor vendor);
}
