package com.stillfresh.app.vendorservice.dto;

import java.time.OffsetDateTime;

public class RatingResponse {
    
    private Long id;
    private Long vendorId;
    private Long userId;
    private Long orderId;
    private Integer collectionProcessRating;
    private Integer qualityRating;
    private Integer quantityRating;
    private Integer varietyRating;
    private Double totalRating; // Average of the 4 categories
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

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

    public Double getTotalRating() {
        return totalRating;
    }

    public void setTotalRating(Double totalRating) {
        this.totalRating = totalRating;
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

