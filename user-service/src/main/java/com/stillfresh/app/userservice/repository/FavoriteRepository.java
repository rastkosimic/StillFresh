package com.stillfresh.app.userservice.repository;

import com.stillfresh.app.userservice.model.Favorite;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Favorite entity.
 * Provides optimized queries for favorite operations.
 */
@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    /**
     * Find a favorite by user ID and offer ID.
     * Used to check if an offer is already favorited.
     */
    Optional<Favorite> findByUserIdAndOfferId(Long userId, Long offerId);

    /**
     * Check if a favorite exists for a user and offer.
     * More efficient than findByUserIdAndOfferId when only checking existence.
     */
    boolean existsByUserIdAndOfferId(Long userId, Long offerId);

    /**
     * Get all favorites for a user, ordered by creation date (newest first).
     */
    List<Favorite> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * Get paginated favorites for a user, ordered by creation date (newest first).
     * Useful for large favorite lists.
     */
    Page<Favorite> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /**
     * Get all offer IDs that a user has favorited.
     * Useful for bulk operations or checking multiple offers at once.
     */
    @Query("SELECT f.offerId FROM Favorite f WHERE f.userId = :userId")
    List<Long> findOfferIdsByUserId(@Param("userId") Long userId);

    /**
     * Count how many favorites a user has.
     */
    long countByUserId(Long userId);

    /**
     * Delete all favorites for a user.
     * Useful when deleting a user account.
     */
    void deleteByUserId(Long userId);

    /**
     * Delete a specific favorite by user ID and offer ID.
     * More efficient than finding and deleting.
     */
    void deleteByUserIdAndOfferId(Long userId, Long offerId);

    /**
     * Count how many users have favorited a specific offer.
     * Useful for analytics.
     */
    long countByOfferId(Long offerId);
}

