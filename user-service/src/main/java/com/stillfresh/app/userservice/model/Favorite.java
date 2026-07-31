package com.stillfresh.app.userservice.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

/**
 * Entity representing a user's favorite offer.
 * This is a many-to-many relationship between User and Offer.
 * 
 * Best Practices:
 * - Unique constraint on (userId, offerId) prevents duplicate favorites
 * - Indexes on userId and offerId for fast queries
 * - Timestamp tracking for analytics and sorting
 */
@Entity
@Table(name = "favorites", 
       uniqueConstraints = {
           @UniqueConstraint(columnNames = {"user_id", "offer_id"}, name = "uk_user_offer")
       },
       indexes = {
           @Index(name = "idx_favorite_user_id", columnList = "user_id"),
           @Index(name = "idx_favorite_offer_id", columnList = "offer_id"),
           @Index(name = "idx_favorite_created_at", columnList = "created_at")
       })
public class Favorite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "offer_id", nullable = false)
    private Long offerId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = OffsetDateTime.now();
    }

    // Constructors
    public Favorite() {
    }

    public Favorite(Long userId, Long offerId) {
        this.userId = userId;
        this.offerId = offerId;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getOfferId() {
        return offerId;
    }

    public void setOfferId(Long offerId) {
        this.offerId = offerId;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "Favorite{" +
                "id=" + id +
                ", userId=" + userId +
                ", offerId=" + offerId +
                ", createdAt=" + createdAt +
                '}';
    }
}

