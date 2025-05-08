package com.stillfresh.app.offerservice.repository;

import com.stillfresh.app.offerservice.model.Offer;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface OfferRepository extends JpaRepository<Offer, Integer> {
	List<Offer> findByVendorIdAndActive(Long vendorId, boolean active);

	@Query("SELECT o FROM Offer o WHERE o.expirationDate < CURRENT_TIMESTAMP AND o.active = true")
	List<Offer> findExpiredOffers();

	@Modifying
	@Transactional
	@Query("UPDATE Offer o SET o.active = false WHERE o.id = :offerId")
	void invalidateOffer(@Param("offerId") int offerId);

	@Modifying
	@Transactional
	@Query("UPDATE Offer o SET o.active = false WHERE o.vendorId = :vendorId AND o.active = true")
	void invalidateAllOffersByVendor(@Param("vendorId") Long vendorId);

	List<Offer> findByVendorId(Long vendorId);

	@Modifying
	@Transactional
	@Query("UPDATE Offer o SET o.address = :address, o.vendorName = :vendorName, o.zipCode = :zipCode, o.latitude = :latitude, "
			+ "o.longitude = :longitude, o.businessType = :businessType, o.pickupStartTime = :pickupStartTime, "
			+ "o.pickupEndTime = :pickupEndTime, o.reviewsCount = :reviewsCount " + "WHERE o.vendorId = :vendorId")
	void updateOfferRelatedVendorDetails(@Param("vendorId") Long vendorId, 
			@Param("vendorName") String vendorName,
			@Param("address") String address,
			@Param("zipCode") String zipCode, 
			@Param("latitude") double latitude,
			@Param("longitude") double longitude,
			@Param("businessType") String businessType, 
			@Param("pickupStartTime") OffsetDateTime pickupStartTime,
			@Param("pickupEndTime") OffsetDateTime pickupEndTime, 
			@Param("reviewsCount") int reviewsCount);

	Optional<Offer> findById(Long offerId);

}
