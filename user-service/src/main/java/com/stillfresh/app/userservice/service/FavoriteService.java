package com.stillfresh.app.userservice.service;

import com.stillfresh.app.sharedentities.dto.OfferDto;
import com.stillfresh.app.sharedentities.exceptions.ResourceNotFoundException;
import com.stillfresh.app.userservice.client.OfferServiceClient;
import com.stillfresh.app.userservice.dto.FavoriteResponse;
import com.stillfresh.app.userservice.dto.FavoritesListResponse;
import com.stillfresh.app.userservice.model.Favorite;
import com.stillfresh.app.userservice.repository.FavoriteRepository;
import com.stillfresh.app.userservice.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Service for managing user favorites.
 * 
 * Best Practices:
 * - Validates offer exists before adding to favorites
 * - Ensures users can only manage their own favorites
 * - Uses transactions for data consistency
 * - Handles duplicate favorites gracefully
 * - Provides pagination for large favorite lists
 */
@Service
public class FavoriteService {

    private static final Logger logger = LoggerFactory.getLogger(FavoriteService.class);

    @Autowired
    private FavoriteRepository favoriteRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired(required = false)
    private OfferServiceClient offerServiceClient;

    /**
     * Add an offer to user's favorites.
     * 
     * @param userId The authenticated user's ID
     * @param offerId The offer ID to favorite
     * @return FavoriteResponse with favorite details
     * @throws ResourceNotFoundException if offer doesn't exist
     * @throws IllegalArgumentException if favorite already exists
     */
    @Transactional
    public FavoriteResponse addFavorite(Long userId, Long offerId) {
        logger.info("Adding favorite: userId={}, offerId={}", userId, offerId);

        // Validate user exists (userId comes from authenticated context, but validate for safety)
        if (!userRepository.findById(userId).isPresent()) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }

        // Check if already favorited (idempotent operation)
        if (favoriteRepository.existsByUserIdAndOfferId(userId, offerId)) {
            logger.warn("Offer {} is already in favorites for user {}", offerId, userId);
            // Return existing favorite instead of throwing error (idempotent)
            Favorite existing = favoriteRepository.findByUserIdAndOfferId(userId, offerId)
                    .orElseThrow(() -> new IllegalStateException("Favorite exists but could not be retrieved"));
            return toFavoriteResponse(existing);
        }

        // Validate offer exists (optional - can be done asynchronously or skipped for performance)
        if (offerServiceClient != null) {
            try {
                OfferDto offer = offerServiceClient.getOfferById(offerId);
                if (offer == null) {
                    throw new ResourceNotFoundException("Offer not found with id: " + offerId);
                }
                logger.debug("Validated offer exists: {}", offerId);
            } catch (Exception e) {
                logger.error("Failed to validate offer existence: {}", e.getMessage());
                throw new ResourceNotFoundException("Offer not found with id: " + offerId);
            }
        } else {
            logger.warn("OfferServiceClient not available, skipping offer validation");
        }

        // Create and save favorite
        Favorite favorite = new Favorite(userId, offerId);
        favorite = favoriteRepository.save(favorite);
        
        logger.info("Successfully added favorite: favoriteId={}, userId={}, offerId={}", 
                   favorite.getId(), userId, offerId);

        return toFavoriteResponse(favorite);
    }

    /**
     * Remove an offer from user's favorites.
     * Idempotent operation: succeeds even if favorite doesn't exist.
     * 
     * @param userId The authenticated user's ID
     * @param offerId The offer ID to unfavorite
     */
    @Transactional
    public void removeFavorite(Long userId, Long offerId) {
        logger.info("Removing favorite: userId={}, offerId={}", userId, offerId);

        // Check if favorite exists
        if (!favoriteRepository.existsByUserIdAndOfferId(userId, offerId)) {
            logger.debug("Favorite not found (already removed or never existed): userId={}, offerId={}", userId, offerId);
            // Idempotent: return successfully - desired state (not in favorites) is already achieved
            return;
        }

        // Delete favorite
        favoriteRepository.deleteByUserIdAndOfferId(userId, offerId);
        
        logger.info("Successfully removed favorite: userId={}, offerId={}", userId, offerId);
    }

    /**
     * Get all favorites for a user with offer details.
     * 
     * @param userId The authenticated user's ID
     * @return List of FavoriteResponse with offer details
     */
    public List<FavoriteResponse> getUserFavorites(Long userId) {
        logger.debug("Getting favorites for user: {}", userId);

        List<Favorite> favorites = favoriteRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return buildFavoriteResponsesWithOffers(favorites);
    }

    /**
     * Get paginated favorites for a user with offer details.
     * 
     * @param userId The authenticated user's ID
     * @param pageable Pagination parameters
     * @return Page of FavoriteResponse with offer details
     */
    public Page<FavoriteResponse> getUserFavorites(Long userId, Pageable pageable) {
        logger.debug("Getting paginated favorites for user: {}, page={}, size={}", 
                    userId, pageable.getPageNumber(), pageable.getPageSize());

        Page<Favorite> favorites = favoriteRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        // If offer client is not available, fall back to per-favorite lookup
        if (offerServiceClient == null) {
            return favorites.map(this::toFavoriteResponse);
        }

        List<Favorite> content = favorites.getContent();
        if (content.isEmpty()) {
            return favorites.map(this::toFavoriteResponse);
        }

        try {
            List<Long> offerIds = content.stream()
                    .map(Favorite::getOfferId)
                    .distinct()
                    .collect(Collectors.toList());
            List<OfferDto> offers = offerServiceClient.getOffersByIds(offerIds);
            Map<Long, OfferDto> offerMap = offers.stream()
                    .filter(o -> o.getId() != null)
                    .collect(Collectors.toMap(
                            OfferDto::getId,
                            Function.identity(),
                            (a, b) -> a
                    ));

            return favorites.map(fav -> toFavoriteResponse(fav, offerMap.get(fav.getOfferId())));
        } catch (Exception e) {
            logger.warn("Failed to batch fetch offers for paginated favorites of user {}: {}. Falling back to per-favorite lookup.",
                    userId, e.getMessage());
            return favorites.map(this::toFavoriteResponse);
        }
    }

    /**
     * Get all favorites for a user with offer details and summary counts (expired/sold out).
     * Use this for the favorites screen so the app can show "You have N expired offers" and mark items.
     *
     * @param userId The authenticated user's ID
     * @return FavoritesListResponse with list and expiredCount, soldOutCount
     */
    public FavoritesListResponse getUserFavoritesWithSummary(Long userId) {
        List<FavoriteResponse> list = getUserFavorites(userId);
        return buildFavoritesListResponse(list);
    }

    /**
     * Get paginated favorites with summary counts.
     *
     * @param userId The authenticated user's ID
     * @param pageable Pagination parameters
     * @return FavoritesListResponse with list and counts (counts are for the current page only when paginated)
     */
    public FavoritesListResponse getUserFavoritesWithSummary(Long userId, Pageable pageable) {
        Page<FavoriteResponse> page = getUserFavorites(userId, pageable);
        List<FavoriteResponse> list = page.getContent();
        FavoritesListResponse response = buildFavoritesListResponse(list);
        response.setTotalCount(page.getTotalElements());
        return response;
    }

    private FavoritesListResponse buildFavoritesListResponse(List<FavoriteResponse> list) {
        long expiredCount = 0;
        long soldOutCount = 0;
        for (FavoriteResponse fr : list) {
            if (fr.getOffer() != null) {
                if (fr.getOffer().isExpired()) expiredCount++;
                if (fr.getOffer().isSoldOut()) soldOutCount++;
            }
        }
        FavoritesListResponse response = new FavoritesListResponse();
        response.setFavorites(list);
        response.setTotalCount(list.size());
        response.setExpiredCount(expiredCount);
        response.setSoldOutCount(soldOutCount);
        return response;
    }

    /**
     * Check if an offer is favorited by a user.
     * 
     * @param userId The authenticated user's ID
     * @param offerId The offer ID to check
     * @return true if favorited, false otherwise
     */
    public boolean isFavorited(Long userId, Long offerId) {
        return favoriteRepository.existsByUserIdAndOfferId(userId, offerId);
    }

    /**
     * Get summary counts for a user's favorites (total, expired, sold out).
     * Use this to show a notification like "You have N expired offers in favorites."
     * Fetches offer details for each favorite to compute counts.
     *
     * @param userId The authenticated user's ID
     * @return FavoritesListResponse with totalCount, expiredCount, soldOutCount (favorites list left null/empty)
     */
    public FavoritesListResponse getFavoritesSummary(Long userId) {
        List<FavoriteResponse> fullList = getUserFavorites(userId);
        FavoritesListResponse response = buildFavoritesListResponse(fullList);
        response.setFavorites(List.of()); // Summary only; do not return full list
        return response;
    }

    /**
     * Get count of favorites for a user.
     * 
     * @param userId The authenticated user's ID
     * @return Number of favorites
     */
    public long getFavoriteCount(Long userId) {
        return favoriteRepository.countByUserId(userId);
    }

    /**
     * Get all offer IDs that a user has favorited.
     * Useful for bulk operations.
     * 
     * @param userId The authenticated user's ID
     * @return List of offer IDs
     */
    public List<Long> getFavoriteOfferIds(Long userId) {
        return favoriteRepository.findOfferIdsByUserId(userId);
    }

    /**
     * Convert Favorite entity to FavoriteResponse DTO.
     * Fetches offer details from offer-service.
     */
    private FavoriteResponse toFavoriteResponse(Favorite favorite) {
        FavoriteResponse response = new FavoriteResponse();
        response.setFavoriteId(favorite.getId());
        response.setOfferId(favorite.getOfferId());
        response.setFavoritedAt(favorite.getCreatedAt());

        // Fetch offer details if client is available
        if (offerServiceClient != null) {
            try {
                OfferDto offer = offerServiceClient.getOfferById(favorite.getOfferId());
                response.setOffer(offer);
            } catch (Exception e) {
                logger.warn("Failed to fetch offer details for offerId {}: {}", 
                           favorite.getOfferId(), e.getMessage());
                // Continue without offer details - favorite still valid
            }
        }

        return response;
    }

    /**
     * Convert Favorite entity to FavoriteResponse DTO using a pre-fetched OfferDto.
     * Does not perform any remote calls.
     */
    private FavoriteResponse toFavoriteResponse(Favorite favorite, OfferDto offer) {
        FavoriteResponse response = new FavoriteResponse();
        response.setFavoriteId(favorite.getId());
        response.setOfferId(favorite.getOfferId());
        response.setFavoritedAt(favorite.getCreatedAt());
        response.setOffer(offer);
        return response;
    }

    /**
     * Build FavoriteResponse list for a user's favorites using a single batched offer lookup.
     * Falls back to per-favorite lookup if the offer client is unavailable or batch call fails.
     */
    private List<FavoriteResponse> buildFavoriteResponsesWithOffers(List<Favorite> favorites) {
        if (favorites.isEmpty()) {
            return List.of();
        }

        if (offerServiceClient == null) {
            return favorites.stream()
                    .map(this::toFavoriteResponse)
                    .collect(Collectors.toList());
        }

        try {
            List<Long> offerIds = favorites.stream()
                    .map(Favorite::getOfferId)
                    .distinct()
                    .collect(Collectors.toList());

            List<OfferDto> offers = offerServiceClient.getOffersByIds(offerIds);
            Map<Long, OfferDto> offerMap = offers.stream()
                    .filter(o -> o.getId() != null)
                    .collect(Collectors.toMap(
                            OfferDto::getId,
                            Function.identity(),
                            (a, b) -> a
                    ));

            return favorites.stream()
                    .map(fav -> toFavoriteResponse(fav, offerMap.get(fav.getOfferId())))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.warn("Failed to batch fetch offers for favorites: {}. Falling back to per-favorite lookup.", e.getMessage());
            return favorites.stream()
                    .map(this::toFavoriteResponse)
                    .collect(Collectors.toList());
        }
    }

    /**
     * Delete all favorites for a user.
     * Used when deleting a user account.
     * 
     * @param userId The user ID
     */
    @Transactional
    public void deleteAllFavorites(Long userId) {
        logger.info("Deleting all favorites for user: {}", userId);
        favoriteRepository.deleteByUserId(userId);
        logger.info("Successfully deleted all favorites for user: {}", userId);
    }
}

