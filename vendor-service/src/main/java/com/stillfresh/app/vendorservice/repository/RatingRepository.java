package com.stillfresh.app.vendorservice.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.stillfresh.app.vendorservice.model.Rating;

public interface RatingRepository extends JpaRepository<Rating, Long> {
    
    // Find all ratings for a specific vendor
    List<Rating> findByVendorId(Long vendorId);
    
    // Find all ratings by a specific user
    List<Rating> findByUserId(Long userId);
    
    // Find rating by order (one rating per order)
    Optional<Rating> findByOrderId(Long orderId);

    // Count ratings for a vendor
    long countByVendorId(Long vendorId);

    // Calculate average rating for a vendor
    @Query("SELECT AVG((r.collectionProcessRating + r.qualityRating + r.quantityRating + r.varietyRating) / 4.0) " +
           "FROM Rating r WHERE r.vendorId = :vendorId")
    Double calculateAverageRatingByVendorId(@Param("vendorId") Long vendorId);

    // Check if an order has already been rated
    boolean existsByOrderId(Long orderId);
}

