package com.stillfresh.app.offerservice.repository;

import com.stillfresh.app.offerservice.model.Offer;
import com.stillfresh.app.sharedentities.enums.OfferCategory;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface OfferRepository extends JpaRepository<Offer, Long> {
	List<Offer> findByVendorIdAndActive(Long vendorId, boolean active);

	@Query("SELECT o FROM Offer o WHERE o.expirationDate < CURRENT_TIMESTAMP AND o.active = true")
	List<Offer> findExpiredOffers();

	@Modifying
	@Transactional
    @Query("UPDATE Offer o SET o.active = false WHERE o.id = :offerId")
    void invalidateOffer(@Param("offerId") Long offerId);

	@Modifying
	@Transactional
	@Query("UPDATE Offer o SET o.active = false WHERE o.vendorId = :vendorId AND o.active = true")
	void invalidateAllOffersByVendor(@Param("vendorId") Long vendorId);

	List<Offer> findByVendorId(Long vendorId);
	
	List<Offer> findByCategory(OfferCategory category);
	
	List<Offer> findByCategoryAndActive(OfferCategory category, boolean active);
	
	List<Offer> findByVendorIdAndCategory(Long vendorId, OfferCategory category);

	@Modifying
	@Transactional
	@Query("UPDATE Offer o SET o.locationName = :locationName, o.chainName = :chainName, "
			+ "o.website = :website, o.vendorImageUrl = :vendorImageUrl, "
			+ "o.address = :address, o.zipCode = :zipCode, o.latitude = :latitude, "
			+ "o.longitude = :longitude, o.businessType = :businessType, "
			+ "o.reviewsCount = :reviewsCount, o.rating = :rating, o.currency = :currency WHERE o.vendorId = :vendorId")
	void updateOfferRelatedVendorDetails(@Param("vendorId") Long vendorId,
			@Param("locationName") String locationName,
			@Param("chainName") String chainName,
			@Param("website") String website,
			@Param("vendorImageUrl") String vendorImageUrl,
			@Param("address") String address,
			@Param("zipCode") String zipCode,
			@Param("latitude") double latitude,
			@Param("longitude") double longitude,
			@Param("businessType") String businessType,
			@Param("reviewsCount") int reviewsCount,
			@Param("rating") double rating,
			@Param("currency") String currency);

    /**
     * Returns offers that are "visible" to users, applying core visibility rules in the database:
     * - Active AND (no expirationDate OR expirationDate is in the future) AND quantityAvailable > 0
     *   OR
     * - expiredAt is today OR soldOutAt is today (for recently expired/sold-out offers).
     */
    @Query("""
        SELECT o FROM Offer o
        WHERE
          (
            o.active = true
            AND (o.expirationDate IS NULL OR o.expirationDate > CURRENT_TIMESTAMP)
            AND o.quantityAvailable > 0
          )
          OR (o.expiredAt = CURRENT_DATE OR o.soldOutAt = CURRENT_DATE)
        """)
    List<Offer> findVisibleOffers();

    /**
     * Visible offers additionally constrained to a latitude/longitude bounding box.
     */
    @Query("""
        SELECT o FROM Offer o
        WHERE
          o.latitude BETWEEN :minLat AND :maxLat
          AND o.longitude BETWEEN :minLon AND :maxLon
          AND (
            (
              o.active = true
              AND (o.expirationDate IS NULL OR o.expirationDate > CURRENT_TIMESTAMP)
              AND o.quantityAvailable > 0
            )
            OR (o.expiredAt = CURRENT_DATE OR o.soldOutAt = CURRENT_DATE)
          )
        """)
    List<Offer> findVisibleOffersInBoundingBox(
            @Param("minLat") double minLat,
            @Param("maxLat") double maxLat,
            @Param("minLon") double minLon,
            @Param("maxLon") double maxLon);


}
