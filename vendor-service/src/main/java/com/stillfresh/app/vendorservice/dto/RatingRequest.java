package com.stillfresh.app.vendorservice.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class RatingRequest {
    
    @NotNull(message = "Vendor ID is required")
    private Long vendorId;
    
    @Min(value = 1, message = "Collection process rating must be between 1 and 5")
    @Max(value = 5, message = "Collection process rating must be between 1 and 5")
    @NotNull(message = "Collection process rating is required")
    private Integer collectionProcessRating;
    
    @Min(value = 1, message = "Quality rating must be between 1 and 5")
    @Max(value = 5, message = "Quality rating must be between 1 and 5")
    @NotNull(message = "Quality rating is required")
    private Integer qualityRating;
    
    @Min(value = 1, message = "Quantity rating must be between 1 and 5")
    @Max(value = 5, message = "Quantity rating must be between 1 and 5")
    @NotNull(message = "Quantity rating is required")
    private Integer quantityRating;
    
    @Min(value = 1, message = "Variety rating must be between 1 and 5")
    @Max(value = 5, message = "Variety rating must be between 1 and 5")
    @NotNull(message = "Variety rating is required")
    private Integer varietyRating;

    @NotNull(message = "Order ID is required")
    private Long orderId;

    // Getters and Setters

    public Long getVendorId() {
        return vendorId;
    }

    public void setVendorId(Long vendorId) {
        this.vendorId = vendorId;
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

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }
}

