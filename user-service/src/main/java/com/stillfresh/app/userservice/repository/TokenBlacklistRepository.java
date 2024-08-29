package com.stillfresh.app.userservice.repository;

import com.stillfresh.app.userservice.model.TokenBlacklistEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TokenBlacklistRepository extends JpaRepository<TokenBlacklistEntry, Long> {

    boolean existsByToken(String token);
}
