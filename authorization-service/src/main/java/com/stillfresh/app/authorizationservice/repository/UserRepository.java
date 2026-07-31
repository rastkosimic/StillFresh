package com.stillfresh.app.authorizationservice.repository;

import com.stillfresh.app.authorizationservice.model.User;
import com.stillfresh.app.sharedentities.enums.Role;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /** Single-query lookup by identifier (email or username), case-insensitive. */
    @Query("SELECT u FROM User u WHERE LOWER(u.email) = LOWER(:identifier) OR LOWER(u.username) = LOWER(:identifier)")
    Optional<User> findByEmailOrUsernameIgnoreCase(@Param("identifier") String identifier);

    Optional<User> findByUsername(String username);
    Optional<User> findByUsernameIgnoreCase(String username);  // Case-insensitive username lookup
    Optional<User> findByEmail(String email);
    Optional<User> findByEmailIgnoreCase(String email);  // Case-insensitive email lookup
    Optional<User> findByOauth2ProviderAndOauth2ProviderId(String provider, String providerId);
    
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    boolean existsByRole(Role role);
}
