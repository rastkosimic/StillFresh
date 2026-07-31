package com.stillfresh.app.vendorservice.model;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Index;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "ratings", 
    indexes = {
        @Index(name = "idx_rating_vendor_id", columnList = "vendor_id"),
        @Index(name = "idx_rating_user_id", columnList = "user_id"),
        @Index(name = "idx_rating_order_id", columnList = "order_id")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_rating_order", columnNames = {"order_id"})
    })
public class Rating {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Vendor ID is required")
    @Column(name = "vendor_id", nullable = false)
    private Long vendorId;

    @NotNull(message = "User ID is required")
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @NotNull(message = "Order ID is required")
    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @NotNull(message = "Collection process rating is required")
    @Min(value = 1, message = "Rating must be at least 1")
    @Max(value = 5, message = "Rating must be at most 5")
    @Column(name = "collection_process_rating", nullable = false)
    private Integer collectionProcessRating;

    @NotNull(message = "Quality rating is required")
    @Min(value = 1, message = "Rating must be at least 1")
    @Max(value = 5, message = "Rating must be at most 5")
    @Column(name = "quality_rating", nullable = false)
    private Integer qualityRating;

    @NotNull(message = "Quantity rating is required")
    @Min(value = 1, message = "Rating must be at least 1")
    @Max(value = 5, message = "Rating must be at most 5")
    @Column(name = "quantity_rating", nullable = false)
    private Integer quantityRating;

    @NotNull(message = "Variety rating is required")
    @Min(value = 1, message = "Rating must be at least 1")
    @Max(value = 5, message = "Rating must be at most 5")
    @Column(name = "variety_rating", nullable = false)
    private Integer varietyRating;

    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(nullable = true)
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = OffsetDateTime.now();
    }

    @jakarta.persistence.PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    // Calculate total rating as average of the 4 categories
    public double getTotalRating() {
        return (collectionProcessRating + qualityRating + quantityRating + varietyRating) / 4.0;
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getVendorId() {
        return vendorId;
    }

    public void setVendorId(Long vendorId) {
        this.vendorId = vendorId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public Integer getCollectionProcessRating() {
        return collectionProcessRating;
    }

    public void setCollectionProcessRating(Integer collectionProcessRating) {
        this.collectionProcessRating = collectionProcessRating;
    }

    public Integer getQualityRating() {
        return qualityRating;
    }

    public void setQualityRating(Integer qualityRating) {
        this.qualityRating = qualityRating;
    }

    public Integer getQuantityRating() {
        return quantityRating;
    }

    public void setQuantityRating(Integer quantityRating) {
        this.quantityRating = quantityRating;
    }

    public Integer getVarietyRating() {
        return varietyRating;
    }

    public void setVarietyRating(Integer varietyRating) {
        this.varietyRating = varietyRating;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}

