package com.stillfresh.app.vendorservice.service;

import com.stillfresh.app.sharedentities.dto.OfferDto;
import com.stillfresh.app.sharedentities.enums.Role;
import com.stillfresh.app.sharedentities.enums.Status;
import com.stillfresh.app.sharedentities.offer.events.AllOffersInvalidationEvent;
import com.stillfresh.app.sharedentities.offer.events.OfferCreationEvent;
import com.stillfresh.app.sharedentities.offer.events.OfferInvalidationEvent;
import com.stillfresh.app.sharedentities.offer.events.OfferUpdateEvent;
import com.stillfresh.app.sharedentities.shared.events.TokenRequestEvent;
import com.stillfresh.app.sharedentities.vendor.events.OfferRelatedVendorDetailsEvent;
import com.stillfresh.app.sharedentities.vendor.events.UpdateVendorProfileEvent;
import com.stillfresh.app.sharedentities.vendor.events.VendorRegisteredEvent;
import com.stillfresh.app.sharedentities.vendor.events.VendorVerifiedEvent;
import com.stillfresh.app.vendorservice.client.OfferClient;
import com.stillfresh.app.vendorservice.dto.PasswordChangeRequest;
import com.stillfresh.app.vendorservice.model.PasswordResetToken;
import com.stillfresh.app.vendorservice.model.Vendor;
import com.stillfresh.app.vendorservice.model.VendorVerificationToken;
import com.stillfresh.app.vendorservice.publisher.VendorEventPublisher;
import com.stillfresh.app.vendorservice.repository.PasswordResetTokenRepository;
import com.stillfresh.app.vendorservice.repository.VendorRepository;
import com.stillfresh.app.vendorservice.repository.VendorVerificationTokenRepository;
import com.stillfresh.app.vendorservice.security.CustomVendorDetails;
import com.stillfresh.app.vendorservice.security.JwtUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.Date;
import java.util.List;

@Service
public class VendorService {

    @Autowired
    private VendorRepository vendorRepository;

    @Autowired
    private VendorVerificationTokenRepository vendorVerificationTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EmailService emailService;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;
    
    @Autowired
    private TokenBlacklistService tokenBlacklistService;

    @Autowired
    private GeoLocationService geoLocationService;
    
    @Autowired
    JwtUtil jwtUtil;
    
    @Autowired
    private VendorEventPublisher eventPublisher;
    
    @Autowired
    private OfferClient offerClient;

    private static final Logger logger = LoggerFactory.getLogger(VendorService.class);
    
    public boolean hasAdmin() {
        // Check if any vendor has the ADMIN role
        return vendorRepository.existsByRole(Role.ADMIN);
    }

    public Vendor registerAdmin(Vendor vendor) throws IOException {
        // Basic validation to prevent duplicate emails or names
        if (vendorRepository.existsByEmail(vendor.getEmail())) {
            throw new RuntimeException("Email is already registered.");
        }

        vendor.setPassword(passwordEncoder.encode(vendor.getPassword()));
        vendor.setRole(Role.ADMIN);  // Assign the ADMIN role
        vendor.setStatus(Status.ACTIVE);

        // Save the admin to the database
        return vendorRepository.save(vendor);
    }

    public Vendor registerVendor(Vendor vendor) throws IOException {
        if (vendorRepository.existsByEmail(vendor.getEmail())) {
            throw new RuntimeException("Vendor already registered with this email");
        }     
        vendor.setPassword(passwordEncoder.encode(vendor.getPassword()));
        vendor.setRole(Role.VENDOR);
        vendor.setStatus(Status.INACTIVE);  // Inactive until verified
        
        // Fetch and assign geo-coordinates with fallback
        try {
            double[] coordinates = geoLocationService.getCoordinates(vendor.getAddress(), vendor.getZipCode());
            if (coordinates != null) {
                vendor.setLatitude(coordinates[0]);
                vendor.setLongitude(coordinates[1]);
                logger.info("Successfully set coordinates for vendor: {}", vendor.getEmail());
            } else {
                logger.warn("Could not geocode address for vendor: {}. Setting default coordinates.", vendor.getEmail());
                // Set default coordinates or leave as null
                vendor.setLatitude(0.0);
                vendor.setLongitude(0.0);
            }
        } catch (Exception e) {
            logger.error("Geocoding failed for vendor: {}. Proceeding with registration without coordinates.", vendor.getEmail(), e);
            // Continue with registration even if geocoding fails
            vendor.setLatitude(0.0);
            vendor.setLongitude(0.0);
        }
        
        vendorRepository.save(vendor);

        // Generate verification token
        String token = UUID.randomUUID().toString();
        VendorVerificationToken verificationToken = new VendorVerificationToken();
        verificationToken.setToken(token);
        verificationToken.setVendor(vendor);
        vendorVerificationTokenRepository.save(verificationToken);

        // Send verification email
        
        String verificationUrl = "http://localhost:8083/vendors/verify?token=" + token;
        emailService.sendVerificationEmail(vendor.getEmail(), verificationUrl);
        
        //Creating an event that will be utilized by authorization-service
        eventPublisher.publishVendorRegisteredEvent(new VendorRegisteredEvent(vendor.getEmail(), vendor.getPassword(), vendor.getStatus(), vendor.getRole(), vendor.getUsername()));
        
        return vendor;
    }

    public boolean verifyVendor(String token) {
        VendorVerificationToken verificationToken = vendorVerificationTokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid token"));
        Vendor vendor = verificationToken.getVendor();
        vendor.setStatus(Status.ACTIVE);
        vendorRepository.save(vendor);
        
        // Publish VendorVerifiedEvent after successful verification. This will be utilized by authorization-service
        eventPublisher.publishVendorVerifiedEvent(new VendorVerifiedEvent(vendor.getEmail())); //treba napraviti event za user verified
        return true;
    }
    
    @CachePut(value = "vendor", key = "#email")
    public void cacheVendorOnLogin(String email) {
    	findVendorByEmail(email);
    }
    
    @Cacheable(value = "vendor", key = "#email", unless = "#result == null")
    private Optional<Vendor> findVendorByEmail(String email) {
    	return vendorRepository.findByEmail(email);	
	}

	public void sendPasswordResetLink(String email) throws IOException {
        Vendor vendor = vendorRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Vendor not found with email: " + email));

        // Check if a token already exists for this vendor and remove it
        Optional<PasswordResetToken> existingToken = passwordResetTokenRepository.findByVendor(vendor);
        existingToken.ifPresent(token -> passwordResetTokenRepository.delete(token));

        // Generate a new reset token
        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setToken(token);
        resetToken.setVendor(vendor);
        resetToken.setExpiryDate(calculateExpiryDate(24)); // Token valid for 24 hours
        passwordResetTokenRepository.save(resetToken);

        // Send the reset email
        emailService.sendPasswordResetEmail(vendor.getEmail(), token);
    }

    public void resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid or expired password reset token"));

        Vendor vendor = resetToken.getVendor();
        logger.info("Old Password Hash: " + vendor.getPassword());
        
        // Hash the new password
        String encodedPassword = passwordEncoder.encode(newPassword);
        vendor.setPassword(encodedPassword);

        logger.info("New Password Hash: " + encodedPassword);
        vendorRepository.save(vendor);

        // Remove the reset token after successful password reset
        passwordResetTokenRepository.delete(resetToken);
    }
    
    @Cacheable(value = "vendor", key = "#id", unless = "#result == null")
    public Vendor findVendorById(Long id) {
        Optional<Vendor> vendor = vendorRepository.findById(id);
        logger.info("Finding a vendor {}, with id: {}", vendor.map(Vendor::getUsername).orElse("Not found"), id);
        return vendor.orElseThrow(() -> new RuntimeException("Vendor not found"));
    }


    private Date calculateExpiryDate(int hours) {
        Date now = new Date();
        return new Date(now.getTime() + (hours * 60 * 60 * 1000));  // Expiry time in milliseconds
    }
    
    private String extractTokenFromContext() {
        String authorizationHeader = SecurityContextHolder.getContext().getAuthentication().getDetails().toString();
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new RuntimeException("Invalid Authorization header");
        }
        return authorizationHeader.substring(7); // Remove "Bearer " prefix
    }

    private Vendor getVendorFromContext() {
        return extractVendorFromToken("Bearer " + extractTokenFromContext());
    }

    public void updateVendorProfile(Vendor updatedVendor) {
        Vendor currentVendor = getVendorFromContext();
        // Update only non-null fields
        if (updatedVendor.getUsername() != null) {
            currentVendor.setUsername(updatedVendor.getUsername());
        }
        boolean addressChanged = false;
        if (updatedVendor.getAddress() != null) {
            currentVendor.setAddress(updatedVendor.getAddress());
            addressChanged = true;
        }
        if (updatedVendor.getPhone() != null) {
            currentVendor.setPhone(updatedVendor.getPhone());
        }
        if (updatedVendor.getPassword() != null) {
            currentVendor.setPassword(passwordEncoder.encode(updatedVendor.getPassword()));
        }
        if (updatedVendor.getRole() != null) {
            currentVendor.setRole(updatedVendor.getRole());
        }
        if (updatedVendor.getStatus() != null) {
            currentVendor.setStatus(updatedVendor.getStatus());
        }
        if (updatedVendor.getBusinessType() != null) {
            currentVendor.setBusinessType(updatedVendor.getBusinessType());
        }
        if (updatedVendor.getOperatingHours() != null) {
            currentVendor.setOperatingHours(updatedVendor.getOperatingHours());
        }
        if (updatedVendor.getSurplusFoodDetails() != null) {
            currentVendor.setSurplusFoodDetails(updatedVendor.getSurplusFoodDetails());
        }
 
        if (updatedVendor.getPricingInfo() != null) {
            currentVendor.setPricingInfo(updatedVendor.getPricingInfo());
        }
        if (updatedVendor.getEnvironmentalCertifications() != null) {
            currentVendor.setEnvironmentalCertifications(updatedVendor.getEnvironmentalCertifications());
        }
        if (updatedVendor.getAverageRating() != 0) {
            currentVendor.setAverageRating(updatedVendor.getAverageRating());
        }
        if (updatedVendor.getReviewsCount() != 0) {
            currentVendor.setReviewsCount(updatedVendor.getReviewsCount());
        }
        if (updatedVendor.getImageUrl() != null) {
            currentVendor.setImageUrl(updatedVendor.getImageUrl());
        }
        boolean zipChanged = false;
        if (updatedVendor.getZipCode() != null) {
            currentVendor.setZipCode(updatedVendor.getZipCode());
            zipChanged = true;
        }

        if (addressChanged || zipChanged) {
            double[] coordinates = geoLocationService.getCoordinates(currentVendor.getAddress(), currentVendor.getZipCode());
            if (coordinates != null) {
                currentVendor.setLatitude(coordinates[0]);
                currentVendor.setLongitude(coordinates[1]);
            }
        }

        vendorRepository.save(currentVendor);
        
        eventPublisher.publishUpdateVendorProfileEvent(new UpdateVendorProfileEvent(currentVendor.getUsername(), currentVendor.getEmail(), currentVendor.getPassword(), currentVendor.getRole(), currentVendor.getStatus()));
        eventPublisher.publishOfferRelatedVendorDetailsEvent(new OfferRelatedVendorDetailsEvent(currentVendor.getId(), currentVendor.getUsername(), currentVendor.getAddress(), currentVendor.getZipCode(), currentVendor.getLatitude(), currentVendor.getLongitude(), currentVendor.getBusinessType(), currentVendor.getReviewsCount()));
        logoutAndInvalidateToken(extractTokenFromContext());
    }

    public ResponseEntity<String> deleteVendorProfile() {
        String jwt = extractTokenFromContext();
        Vendor vendor = getVendorFromContext();

        vendor.setStatus(Status.DELETED);
        vendorRepository.save(vendor);
        
        eventPublisher.publishUpdateVendorProfileEvent(new UpdateVendorProfileEvent(vendor.getUsername(), vendor.getEmail(), vendor.getPassword(), vendor.getRole(), vendor.getStatus()));
        eventPublisher.publishTokenInvalidationRequest(new TokenRequestEvent(jwt, null));
        eventPublisher.invalidateAllOffers(new AllOffersInvalidationEvent(vendor.getId()));
        return ResponseEntity.ok("Vendor deleted successfully");
    }

    public void createOffer(OfferDto request) {
        Vendor vendor = getVendorFromContext();

        OfferCreationEvent event = new OfferCreationEvent(vendor.getId(), vendor.getUsername(),request.getName(), request.getDescription(), request.getPrice(), request.getOriginalPrice(), request.getQuantityAvailable(), vendor.getAddress(), vendor.getZipCode(), vendor.getLatitude(), vendor.getLongitude(), 
                vendor.getBusinessType(), request.getDietaryInfo(), request.getAllergenInfo(), request.getPickupStartTime(), request.getPickupEndTime(), request.getImageUrl(), request.getRating(), request.getReviewsCount(), request.getExpirationDate());
        eventPublisher.publishOfferCreationEvent(event);
    }

    public List<OfferDto> getActiveOffersForVendor() {
        return offerClient.getActiveOffersForVendor(getVendorFromContext().getId());
    }
    
    public List<OfferDto> getAllOffersForVendor() {
        return offerClient.getAllOffersForVendor(getVendorFromContext().getId());
    }

    public void updateOffer(Long offerId, OfferDto request) {
        Vendor vendor = getVendorFromContext();
        OfferDto offerDto = new OfferDto(offerId, vendor.getUsername(), request.getName(), request.getDescription(), request.getPrice(), request.getOriginalPrice(), 
                request.getQuantityAvailable(), request.getDietaryInfo(), request.getAllergenInfo(), request.getImageUrl(), request.getRating(), 
                vendor.getReviewsCount(), request.getExpirationDate(), true, request.getCreatedAt(), vendor.getAddress(), vendor.getZipCode(), 
                vendor.getLatitude(), vendor.getLongitude(), vendor.getBusinessType(), request.getPickupStartTime(), request.getPickupEndTime());
        
        OfferUpdateEvent event = new OfferUpdateEvent(vendor.getId(), offerDto);
        eventPublisher.publishUpdateOfferEvent(event);
    }

    public void invalidateOffer(int offerId) {
        eventPublisher.publishOfferInvalidationEvent(new OfferInvalidationEvent(offerId));
    }

	public ResponseEntity<String> changeVendorPassword(Vendor vendor, PasswordChangeRequest passwordChangeRequest) {
	
	    if (!passwordEncoder.matches(passwordChangeRequest.getOldPassword(), vendor.getPassword())) {
	        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Old password is incorrect");
	    }
	
	    // Change password and save
	    String encodedPassword = passwordEncoder.encode(passwordChangeRequest.getNewPassword());
	    vendor.setPassword(encodedPassword);
	    vendorRepository.save(vendor);
	
	    return ResponseEntity.ok("Password changed successfully");
	}
	
	public void logoutAndInvalidateToken(String jwt) {
	    long expiryDurationInMillis = jwtUtil.getExpirationTimeInMillis(jwt) - System.currentTimeMillis();
	    tokenBlacklistService.addTokenToBlacklist(jwt, expiryDurationInMillis);
	    
	    // Clear the security context (forces logout)
	    SecurityContextHolder.clearContext();
	}
	
    // Admin-only methods for vendor management
	
    // Check if a Super-Admin exists
    public boolean hasSuperAdmin() {
        return vendorRepository.existsByRole(Role.SUPER_ADMIN);
    }

    // Method to register the first Super-Admin
    public Vendor registerSuperAdmin(Vendor admin) {
        if (hasSuperAdmin()) {
            throw new RuntimeException("Super-Admin already exists.");
        }

        admin.setPassword(passwordEncoder.encode(admin.getPassword()));
        admin.setRole(Role.SUPER_ADMIN);
        admin.setStatus(Status.ACTIVE);; // Automatically activate the Super-Admin
        return vendorRepository.save(admin);
    }
	
    // Method to delete an admin (restricted to Super-Admin)
    public void deleteAdminById(Long id) {
        Vendor admin = findVendorById(id);

        // Prevent deleting Super-Admin
        if (admin.getRole() == Role.SUPER_ADMIN) {
            throw new RuntimeException("Cannot delete a Super-Admin.");
        }

        // Allow only the deletion of regular Admins
        if (admin.getRole() != Role.ADMIN) {
            throw new RuntimeException("This user is not an admin and cannot be deleted as such.");
        }

        vendorRepository.deleteById(id);
    }
    
    public Vendor registerVendor(Vendor vendor, boolean isAdmin) throws IOException {    	
        if (vendorRepository.existsByEmail(vendor.getEmail())) {
            throw new RuntimeException("Vendor already registered with this email");
        }
        vendor.setPassword(passwordEncoder.encode(vendor.getPassword()));
        vendor.setRole(isAdmin ? Role.ADMIN : Role.VENDOR);  // Set role based on input
        vendor.setStatus(Status.INACTIVE);  // Inactive until verified
        
        // Fetch and assign geo-coordinates
        double[] coordinates = geoLocationService.getCoordinates(vendor.getAddress(), vendor.getZipCode());
        if (coordinates != null) {
            vendor.setLatitude(coordinates[0]);
            vendor.setLongitude(coordinates[1]);
        }
        
        vendorRepository.save(vendor);

      // Generate verification token
      String token = UUID.randomUUID().toString();
      VendorVerificationToken verificationToken = new VendorVerificationToken();
      verificationToken.setToken(token);
      verificationToken.setVendor(vendor);
      vendorVerificationTokenRepository.save(verificationToken);

      // Send verification email
      
      String verificationUrl = "http://localhost:8083/vendors/verify?token=" + token;
      emailService.sendVerificationEmail(vendor.getEmail(), verificationUrl);
      
      //Creating an event that will be utilized by authorization-service
      eventPublisher.publishVendorRegisteredEvent(new VendorRegisteredEvent(vendor.getEmail(), vendor.getPassword(), vendor.getStatus(), vendor.getRole(), vendor.getUsername()));

        return vendor;
    }

    // Promote existing vendor to admin
    public void promoteVendorToAdmin(Long vendorId) {
        Vendor vendor = findVendorById(vendorId);

        // Check if the vendor is already an admin
        if (vendor.getRole() == Role.ADMIN || vendor.getRole() == Role.SUPER_ADMIN) {
            throw new RuntimeException("Vendor is already an admin or super-admin");
        }

        vendor.setRole(Role.ADMIN);
        vendorRepository.save(vendor);
    }

    public List<Vendor> getAllVendors() {
        return vendorRepository.findAll();
    }
    
	@Cacheable(value = "vendor", key = "#email", unless = "#result == null")
	public Optional<Vendor> findByEmail(String email) {
		return vendorRepository.findByEmail(email);
	}

    public boolean toggleVendorActivation(Long id) {
        Vendor vendor = findVendorById(id);
        vendor.setStatus(vendor.getStatus() == Status.ACTIVE ? Status.INACTIVE : Status.ACTIVE);
        vendorRepository.save(vendor);
        return vendor.isActive();
    }

    public void deleteVendorById(Long id) {
        Vendor vendor = findVendorById(id);

        // Only allow deletion of vendors with the VENDOR role
        if (vendor.getRole() != Role.VENDOR) {
            throw new RuntimeException("You are not allowed to delete this user. Only users with the VENDOR role can be deleted.");
        }

        // Perform the deletion
        vendorRepository.deleteById(id);
    }


    // Method to activate a vendor
    public boolean activateVendor(Long id) {
        Vendor vendor = findVendorById(id);
        vendor.setStatus(Status.ACTIVE); // Set active to true
        vendorRepository.save(vendor);
        return vendor.isActive();
    }

    // Method to deactivate a vendor
    public boolean deactivateVendor(Long id) {
        Vendor vendor = findVendorById(id);
        vendor.setStatus(Status.INACTIVE); // Set active to false
        vendorRepository.save(vendor);
        return !vendor.isActive();
    }
    
    public void demoteVendorFromAdmin(Long id) {
        Vendor vendor = findVendorById(id);

        if (vendor.getRole() != Role.ADMIN) {
            throw new IllegalArgumentException("This vendor is not an admin.");
        }

        // Set the role back to VENDOR
        vendor.setRole(Role.VENDOR);
        vendorRepository.save(vendor);
    }
    
    private Vendor extractVendorFromToken(String authorizationHeader) {
	    if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
	        throw new RuntimeException("Invalid Authorization header");
	    }
	    String jwt = authorizationHeader.substring(7); // Remove "Bearer " prefix
	    
	    String email = jwtUtil.extractEmail(jwt);
		
	    // Retrieve the user from the cache
	    Optional<Vendor> cachedVendor = findByEmail(email);
	    if (cachedVendor.isEmpty()) {
	        throw new RuntimeException("Vendor not found in cache");
	    }

	    return cachedVendor.get();
    }

}
