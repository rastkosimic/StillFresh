package com.stillfresh.app.userservice.dto;

import com.stillfresh.app.sharedentities.dto.OfferDto;
import java.time.OffsetDateTime;

/**
 * DTO for favorite response.
 * Contains the offer details and when it was favorited.
 */
public class FavoriteResponse {
    private Long favoriteId;
    private Long offerId;
    private OffsetDateTime favoritedAt;
    private OfferDto offer; // Full offer details

    public FavoriteResponse() {
    }

    public FavoriteResponse(Long favoriteId, Long offerId, OffsetDateTime favoritedAt, OfferDto offer) {
        this.favoriteId = favoriteId;
        this.offerId = offerId;
        this.favoritedAt = favoritedAt;
        this.offer = offer;
    }

    // Getters and Setters
    public Long getFavoriteId() {
        return favoriteId;
    }

    public void setFavoriteId(Long favoriteId) {
        this.favoriteId = favoriteId;
    }

    public Long getOfferId() {
        return offerId;
    }

    public void setOfferId(Long offerId) {
        this.offerId = offerId;
    }

    public OffsetDateTime getFavoritedAt() {
        return favoritedAt;
    }

    public void setFavoritedAt(OffsetDateTime favoritedAt) {
        this.favoritedAt = favoritedAt;
    }

    public OfferDto getOffer() {
        return offer;
    }

    public void setOffer(OfferDto offer) {
        this.offer = offer;
    }
}

