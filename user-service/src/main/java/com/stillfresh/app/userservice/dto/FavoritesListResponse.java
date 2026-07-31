package com.stillfresh.app.userservice.dto;

import java.util.List;

/**
 * Wrapper for GET /favorites response.
 * Includes the list of favorites plus summary counts so the app can notify
 * the user about expired or sold-out offers in favorites.
 */
public class FavoritesListResponse {
    private List<FavoriteResponse> favorites;
    private long totalCount;
    private long expiredCount;
    private long soldOutCount;

    public FavoritesListResponse() {
    }

    public FavoritesListResponse(List<FavoriteResponse> favorites, long totalCount, long expiredCount, long soldOutCount) {
        this.favorites = favorites;
        this.totalCount = totalCount;
        this.expiredCount = expiredCount;
        this.soldOutCount = soldOutCount;
    }

    public List<FavoriteResponse> getFavorites() {
        return favorites;
    }

    public void setFavorites(List<FavoriteResponse> favorites) {
        this.favorites = favorites;
    }

    public long getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(long totalCount) {
        this.totalCount = totalCount;
    }

    public long getExpiredCount() {
        return expiredCount;
    }

    public void setExpiredCount(long expiredCount) {
        this.expiredCount = expiredCount;
    }

    public long getSoldOutCount() {
        return soldOutCount;
    }

    public void setSoldOutCount(long soldOutCount) {
        this.soldOutCount = soldOutCount;
    }
}
