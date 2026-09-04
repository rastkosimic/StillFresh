package com.stillfresh.app.vendorservice.service;

import com.stillfresh.app.sharedentities.dto.OfferDto;
import com.stillfresh.app.sharedentities.enums.ManualPayoutMethod;
import com.stillfresh.app.sharedentities.enums.PaymentProvider;
import com.stillfresh.app.sharedentities.enums.PayoutModel;
import com.stillfresh.app.sharedentities.enums.Role;
import com.stillfresh.app.sharedentities.enums.Status;
import com.stillfresh.app.sharedentities.offer.events.AllOffersInvalidationEvent;
import com.stillfresh.app.sharedentities.offer.events.OfferCreationEvent;
import com.stillfresh.app.sharedentities.offer.events.OfferInvalidationEvent;
import com.stillfresh.app.sharedentities.offer.events.OfferUpdateEvent;
import com.stillfresh.app.sharedentities.shared.events.TokenRequestEvent; // Still needed for token invalidation
import com.stillfresh.app.sharedentities.vendor.events.BankingModelChangedEvent;
import com.stillfresh.app.sharedentities.vendor.events.OfferRelatedVendorDetailsEvent;
import com.stillfresh.app.sharedentities.user.events.PasswordUpdateEvent;
import com.stillfresh.app.sharedentities.vendor.events.UpdateVendorProfileEvent;
import com.stillfresh.app.sharedentities.vendor.events.VendorRegisteredEvent;
import com.stillfresh.app.sharedentities.vendor.events.VendorVerifiedEvent;
import com.stillfresh.app.sharedentities.dto.VendorStatsResponse;
import com.stillfresh.app.vendorservice.client.AuthorizationServiceClient;
import com.stillfresh.app.vendorservice.client.OfferClient;
import com.stillfresh.app.vendorservice.client.OrderClient;
import com.stillfresh.app.vendorservice.dto.ChainLocationStatsResponse;
import com.stillfresh.app.vendorservice.dto.DeleteVendorAccountRequest;
import com.stillfresh.app.vendorservice.dto.PasswordChangeRequest;
import com.stillfresh.app.vendorservice.dto.VendorProfileUpdateRequest;
import com.stillfresh.app.vendorservice.model.PasswordResetToken;
import com.stillfresh.app.vendorservice.model.Vendor;
import com.stillfresh.app.vendorservice.model.VendorBalanceTransaction;
import com.stillfresh.app.vendorservice.model.VendorDeletionFeedback;
import com.stillfresh.app.vendorservice.model.VendorPayout;
import com.stillfresh.app.vendorservice.model.VendorVerificationToken;
import com.stillfresh.app.vendorservice.publisher.VendorEventPublisher;
import com.stillfresh.app.vendorservice.repository.PasswordResetTokenRepository;
import com.stillfresh.app.vendorservice.repository.VendorBalanceTransactionRepository;
import com.stillfresh.app.vendorservice.repository.VendorPayoutRepository;
import com.stillfresh.app.vendorservice.repository.VendorDeletionFeedbackRepository;
import com.stillfresh.app.vendorservice.repository.VendorRepository;
import com.stillfresh.app.vendorservice.repository.VendorVerificationTokenRepository;
import com.stillfresh.app.vendorservice.security.CustomVendorDetails;
import com.stillfresh.app.vendorservice.security.JwtUtil;
import com.stillfresh.app.vendorservice.listener.VendorStatsResponseListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.Date;

@Service
public class VendorService {

    @Autowired
    private VendorRepository vendorRepository;

    @Autowired
    private VendorDeletionFeedbackRepository vendorDeletionFeedbackRepository;

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
    private CacheManager cacheManager;
    
    @Autowired
    private OfferClient offerClient;

    @Autowired
    private OrderClient orderClient;
    
    @Autowired
    private AuthorizationServiceClient authorizationServiceClient;

    @Autowired
    private VendorStatsResponseListener vendorStatsResponseListener;
    
    @Autowired(required = false)
    private com.stillfresh.app.vendorservice.client.PaymentClient paymentClient;
    
    @Autowired
    private PaymentProviderService paymentProviderService;
    
    @Autowired
    private PaymentRoutingService paymentRoutingService;
    
    @Autowired
    private VendorBalanceTransactionRepository balanceTransactionRepository;
    
    @Autowired
    private TimeZoneDetectionService timeZoneDetectionService;
    
    @Autowired
    private VendorPayoutRepository payoutRepository;
    
    @Autowired
    private CountryCodeConverter countryCodeConverter;

    /** Login URL sent to newly created locations and workers in their credentials email. */
    @Value("${vendor.login-url:http://localhost:8080/auth/login}")
    private String loginUrl;

    /** Upper bound on locations per chain, to keep a compromised admin from mass-creating accounts. */
    @Value("${vendor.chain.max-locations:200}")
    private int maxChainLocations;

    /** Upper bound on workers per location. */
    @Value("${vendor.chain.max-workers-per-location:100}")
    private int maxWorkersPerLocation;

    private static final Logger logger = LoggerFactory.getLogger(VendorService.class);
    
    public boolean hasAdmin() {
        // Check if any vendor has the ADMIN role
        return vendorRepository.existsByRole(Role.ADMIN);
    }
    
    /**
     * Registers a pending vendor account (admin-initiated registration)
     * Creates vendor with PENDING_VERIFICATION status, awaiting admin verification
     * 
     * @param request Pending vendor registration request
     * @return Created vendor entity
     * @throws IOException if email sending fails
     */
    public Vendor registerPendingVendor(com.stillfresh.app.vendorservice.dto.PendingVendorRegistrationRequest request) throws IOException {
        if (vendorRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Vendor already registered with this email: " + request.getEmail());
        }
        
        String username = request.getEmail().split("@")[0] + "_" + System.currentTimeMillis();
        
        // 1. Get global user ID from authorization service
        // Use VENDOR role initially (database constraint may not allow VENDOR_ADMIN)
        // Will be promoted to VENDOR_ADMIN during verification
        logger.info("Requesting global user ID for pending vendor: {}", request.getEmail());
        Map<String, Object> idResponse = authorizationServiceClient.generateUserId(
            new AuthorizationServiceClient.UserIdRequest(
                request.getEmail(), 
                username, 
                Role.VENDOR  // Use VENDOR initially, will be promoted to VENDOR_ADMIN after verification
            )
        );
        
        if (!(Boolean) idResponse.get("success")) {
            throw new RuntimeException("Failed to generate global user ID: " + idResponse.get("message"));
        }
        
        Long globalUserId = ((Number) idResponse.get("globalUserId")).longValue();
        logger.info("Received global user ID: {} for pending vendor: {}", globalUserId, request.getEmail());
        
        // 2. Create vendor entity
        Vendor vendor = new Vendor();
        vendor.setId(globalUserId);
        vendor.setUsername(username);
        vendor.setEmail(request.getEmail());
        vendor.setPhone(request.getPhone());
        vendor.setAddress(request.getBusinessAddress());
        vendor.setLocationName(request.getLocationName());
        vendor.setZipCode(request.getZipCode());
        vendor.setBusinessRegistrationId(request.getBusinessRegistrationId());
        vendor.setContactPerson(request.getContactPerson());

        // Record legal acceptance (version from client, timestamp server-side).
        java.time.LocalDateTime acceptedAt = java.time.LocalDateTime.now();
        if (request.getTermsVersion() != null && !request.getTermsVersion().isBlank()) {
            vendor.setTermsVersion(request.getTermsVersion());
            vendor.setTermsAcceptedAt(acceptedAt);
        }
        if (request.getPrivacyVersion() != null && !request.getPrivacyVersion().isBlank()) {
            vendor.setPrivacyVersion(request.getPrivacyVersion());
            vendor.setPrivacyAcceptedAt(acceptedAt);
        }
        
        // Set initial status and role
        // Use VENDOR role initially (database constraint may not allow VENDOR_ADMIN)
        // Will be promoted to VENDOR_ADMIN during verification
        vendor.setRole(Role.VENDOR);  // Will be promoted to VENDOR_ADMIN after verification
        vendor.setStatus(Status.INACTIVE);  // Inactive until admin verifies
        vendor.setOnboardingStatus(com.stillfresh.app.sharedentities.enums.OnboardingStatus.PENDING_VERIFICATION);
        
        // Set as unique vendor initially (can change during onboarding)
        vendor.setIsUniqueVendor(true);
        vendor.setIsChainLocation(false);
        vendor.setIsHeadquarters(false);
        
        // Coordinates: use values provided by the client if present, otherwise
        // seed with 0.0 and let the vendor set them later during onboarding
        // (PUT /vendors/onboarding/add-headquarters).
        vendor.setLatitude(request.getLatitude() != null ? request.getLatitude() : 0.0);
        vendor.setLongitude(request.getLongitude() != null ? request.getLongitude() : 0.0);
        
        // Generate a temporary password (will be replaced when admin verifies)
        String tempPassword = generateTemporaryPassword();
        vendor.setPassword(passwordEncoder.encode(tempPassword));
        
        // 3. Save vendor
        vendorRepository.save(vendor);
        
        // 4. Update authorization service
        logger.info("Updating credentials in authorization service for pending vendor: {}", globalUserId);
        requireAuthorizationCredentialsSynced(globalUserId, vendor.getPassword(), Status.INACTIVE);
        
        logger.info("Pending vendor registered successfully: {} (ID: {})", request.getEmail(), globalUserId);
        return vendor;
    }
    
    /**
     * Verifies and activates a pending vendor account (admin action)
     * Generates new credentials and sends them to the vendor
     * 
     * @param vendorId ID of the vendor to verify
     * @return VendorCredentialsResponse with login credentials
     * @throws IOException if email sending fails
     */
    public com.stillfresh.app.vendorservice.dto.VendorCredentialsResponse verifyAndActivateVendor(Long vendorId) throws IOException {
        Vendor vendor = findVendorById(vendorId);
        
        // Validate vendor is in pending state
        if (vendor.getOnboardingStatus() != com.stillfresh.app.sharedentities.enums.OnboardingStatus.PENDING_VERIFICATION) {
            throw new RuntimeException("Vendor is not in PENDING_VERIFICATION status. Current status: " + vendor.getOnboardingStatus());
        }
        
        if (vendor.getStatus() != Status.INACTIVE) {
            throw new RuntimeException("Vendor is not inactive. Current status: " + vendor.getStatus());
        }
        
        // Generate new secure password
        String newPassword = generateSecurePassword();
        String encodedPassword = passwordEncoder.encode(newPassword);
        
        // Update vendor
        vendor.setPassword(encodedPassword);
        vendor.setStatus(Status.ACTIVE);
        vendor.setOnboardingStatus(com.stillfresh.app.sharedentities.enums.OnboardingStatus.VERIFIED);
        vendor.setRole(Role.VENDOR_ADMIN);  // Ensure role is VENDOR_ADMIN
        
        // Update authorization service - credentials and role
        logger.info("Updating credentials in authorization service for verified vendor: {}", vendorId);
        requireAuthorizationCredentialsSynced(vendorId, encodedPassword, Status.ACTIVE);
        
        // Update role in authorization service (promote from VENDOR to VENDOR_ADMIN)
        logger.info("Updating role in authorization service for verified vendor: {} to VENDOR_ADMIN", vendorId);
        Map<String, Object> roleResponse = authorizationServiceClient.updateUserRole(
            vendorId, 
            Role.VENDOR_ADMIN
        );
        
        if (!(Boolean) roleResponse.get("success")) {
            logger.error("Failed to update role in authorization service: {}", roleResponse.get("message"));
            // Don't throw exception - role update failure is not critical if credentials were updated
            logger.warn("Continuing with vendor activation despite role update failure");
        }
        
        // Save vendor
        vendorRepository.save(vendor);
        clearVendorCache(vendor.getId(), vendor.getEmail());
        
        // Send credentials email (non-blocking)
        try {
            emailService.sendVendorCredentialsEmail(vendor.getEmail(), vendor.getUsername(), newPassword, loginUrl);
            logger.info("Credentials email sent successfully to vendor: {}", vendor.getEmail());
        } catch (Exception e) {
            logger.warn("Failed to send credentials email to vendor {} (id: {}): {}. Vendor was verified successfully.",
                       vendor.getEmail(), vendorId, e.getMessage());
            // Don't fail verification if email sending fails - vendor is already verified and credentials are in response
        }
        
        logger.info("Vendor verified and activated: {} (ID: {})", vendor.getEmail(), vendorId);
        
        return new com.stillfresh.app.vendorservice.dto.VendorCredentialsResponse(
            vendor.getEmail(),
            newPassword,
            loginUrl,
            "Vendor verified and activated. Credentials sent to email."
        );
    }
    
    /**
     * Generates a temporary password for pending vendors
     */
    private String generateTemporaryPassword() {
        return "Temp" + System.currentTimeMillis() % 10000;  // Simple temp password
    }
    
    /**
     * Generates a secure password for vendor accounts
     */
    private String generateSecurePassword() {
        // Generate a secure 12-character password
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%";
        StringBuilder password = new StringBuilder();
        java.util.Random random = new java.util.Random();
        
        for (int i = 0; i < 12; i++) {
            password.append(chars.charAt(random.nextInt(chars.length())));
        }
        
        return password.toString();
    }
    
    /**
     * Gets all vendors pending verification
     * @return List of vendors with PENDING_VERIFICATION status
     */
    public List<Vendor> getPendingVendors() {
        return vendorRepository.findByOnboardingStatus(
            com.stillfresh.app.sharedentities.enums.OnboardingStatus.PENDING_VERIFICATION
        );
    }
    
    // ========== Vendor Onboarding Flow Methods ==========
    
    /**
     * Step 1: Set vendor type (CHAIN or UNIQUE)
     * Validates current onboarding status and updates vendor type
     */
    @org.springframework.transaction.annotation.Transactional
    public void setVendorType(com.stillfresh.app.vendorservice.dto.VendorTypeRequest request) {
        Vendor vendor = getVendorFromContext();
        
        // Validate onboarding status
        if (vendor.getOnboardingStatus() != com.stillfresh.app.sharedentities.enums.OnboardingStatus.VERIFIED) {
            throw new RuntimeException("Vendor must be VERIFIED before setting vendor type. Current status: " + vendor.getOnboardingStatus());
        }
        
        if (request.getVendorType() == com.stillfresh.app.vendorservice.dto.VendorTypeRequest.VendorType.CHAIN) {
            if (request.getChainName() == null || request.getChainName().trim().isEmpty()) {
                throw new RuntimeException("Chain name is required for CHAIN type");
            }
            
            requireAvailableChainName(request.getChainName().trim(), vendor.getChainId());
            
            // Generate chain ID (UUID format)
            String chainId = UUID.randomUUID().toString();
            vendor.setChainId(chainId);
            vendor.setChainName(request.getChainName().trim());
            vendor.setIsChainLocation(true);
            vendor.setIsUniqueVendor(false);
            vendor.setIsHeadquarters(false);  // Will be set when HQ is added
            vendor.setOnboardingStatus(com.stillfresh.app.sharedentities.enums.OnboardingStatus.TYPE_SELECTED);
            
            logger.info("Vendor {} set as CHAIN: {}", vendor.getEmail(), request.getChainName());
        } else {
            // UNIQUE vendor
            vendor.setIsChainLocation(false);
            vendor.setIsUniqueVendor(true);
            vendor.setIsHeadquarters(false);
            vendor.setChainId(null);
            vendor.setChainName(null);
            vendor.setOnboardingStatus(com.stillfresh.app.sharedentities.enums.OnboardingStatus.TYPE_SELECTED);
            
            logger.info("Vendor {} set as UNIQUE", vendor.getEmail());
        }
        
        vendorRepository.save(vendor);
        clearVendorCache(vendor.getId(), vendor.getEmail());
    }
    
    /**
     * Step 2: Add headquarters location (CHAIN only)
     * Sets current vendor as headquarters with provided location details
     */
    public void addHeadquarters(com.stillfresh.app.vendorservice.dto.HeadquartersRequest request) {
        Vendor vendor = getVendorFromContext();
        
        // Validate vendor is a chain
        if (!Boolean.TRUE.equals(vendor.getIsChainLocation())) {
            throw new RuntimeException("Only CHAIN vendors can add headquarters. Current vendor type: UNIQUE");
        }
        
        // Validate onboarding status
        if (vendor.getOnboardingStatus() != com.stillfresh.app.sharedentities.enums.OnboardingStatus.TYPE_SELECTED) {
            throw new RuntimeException("Vendor type must be selected before adding headquarters. Current status: " + vendor.getOnboardingStatus());
        }
        
        // Update vendor with headquarters information
        vendor.setLocationName(request.getLocationName());
        vendor.setAddress(request.getAddress());
        vendor.setZipCode(request.getZipCode());
        vendor.setLatitude(request.getLatitude());
        vendor.setLongitude(request.getLongitude());
        if (request.getPhone() != null && !request.getPhone().trim().isEmpty()) {
            vendor.setPhone(request.getPhone());
        }
        // Set country (required for payment account setup)
        if (request.getCountry() != null && !request.getCountry().trim().isEmpty()) {
            // Convert country name to ISO 2-letter code
            String countryCode = countryCodeConverter.convertToIsoCode(request.getCountry());
            if (countryCode == null) {
                logger.warn("Could not convert country '{}' to ISO code. Using original value.", request.getCountry());
                countryCode = request.getCountry().trim().toUpperCase();
            }
            vendor.setCountry(countryCode);
            // Set payment provider based on country code
            PaymentProvider provider = paymentProviderService.determineProvider(countryCode);
            PayoutModel payoutModel = paymentProviderService.determinePayoutModel(countryCode);
            boolean stripeSupported = paymentProviderService.isStripeSupported(countryCode);
            vendor.setPaymentProvider(provider);
            vendor.setPayoutModel(payoutModel);
            vendor.setStripeSupported(stripeSupported);
            logger.info("Converted country '{}' to ISO code '{}' for headquarters", request.getCountry(), countryCode);
        }
        vendor.setIsHeadquarters(true);
        vendor.setOnboardingStatus(com.stillfresh.app.sharedentities.enums.OnboardingStatus.HEADQUARTERS_ADDED);
        
        vendorRepository.save(vendor);
        clearVendorCache(vendor.getId(), vendor.getEmail());
        
        logger.info("Headquarters added for chain {}: {}", vendor.getChainName(), request.getLocationName());
    }
    
    /**
     * Step 3: Set banking model (SHARED or INDIVIDUAL)
     * For CHAIN vendors: determines if locations share payment account
     * For UNIQUE vendors: sets up for future chain upgrade
     */
    public void setBankingModel(com.stillfresh.app.vendorservice.dto.BankingModelRequest request) {
        Vendor vendor = getVendorFromContext();
        
        // Validate onboarding status
        com.stillfresh.app.sharedentities.enums.OnboardingStatus currentStatus = vendor.getOnboardingStatus();
        com.stillfresh.app.sharedentities.enums.OnboardingStatus expectedStatus = 
            Boolean.TRUE.equals(vendor.getIsChainLocation()) 
                ? com.stillfresh.app.sharedentities.enums.OnboardingStatus.HEADQUARTERS_ADDED
                : com.stillfresh.app.sharedentities.enums.OnboardingStatus.TYPE_SELECTED;
        
        if (currentStatus != expectedStatus) {
            throw new RuntimeException("Invalid onboarding status. Expected: " + expectedStatus + ", Current: " + currentStatus);
        }
        
        if (request.getBankingModel() == com.stillfresh.app.vendorservice.dto.BankingModelRequest.BankingModel.SHARED) {
            // SHARED banking model
            if (Boolean.TRUE.equals(vendor.getIsChainLocation())) {
                // For chains: HQ will own the shared account
                vendor.setUsesSharedPaymentAccount(true);
                vendor.setSharedPaymentAccountVendorId(vendor.getId());  // HQ owns the account
            } else {
                // For unique vendors: can't use shared model yet (no other locations)
                throw new RuntimeException("UNIQUE vendors cannot use SHARED banking model. Upgrade to chain first or use INDIVIDUAL model.");
            }
        } else {
            // INDIVIDUAL banking model
            vendor.setUsesSharedPaymentAccount(false);
            vendor.setSharedPaymentAccountVendorId(null);
        }
        
        // Set country for UNIQUE vendors (CHAIN vendors should have country set during headquarters step)
        if (!Boolean.TRUE.equals(vendor.getIsChainLocation()) && request.getCountry() != null && !request.getCountry().trim().isEmpty()) {
            // Convert country name to ISO 2-letter code
            String countryCode = countryCodeConverter.convertToIsoCode(request.getCountry());
            if (countryCode == null) {
                logger.warn("Could not convert country '{}' to ISO code. Using original value.", request.getCountry());
                countryCode = request.getCountry().trim().toUpperCase();
            }
            vendor.setCountry(countryCode);
            // Set payment provider based on country code
            PaymentProvider provider = paymentProviderService.determineProvider(countryCode);
            PayoutModel payoutModel = paymentProviderService.determinePayoutModel(countryCode);
            boolean stripeSupported = paymentProviderService.isStripeSupported(countryCode);
            vendor.setPaymentProvider(provider);
            vendor.setPayoutModel(payoutModel);
            vendor.setStripeSupported(stripeSupported);
            logger.info("Converted country '{}' to ISO code '{}' and set payment info for UNIQUE vendor {}", request.getCountry(), countryCode, vendor.getEmail());
        }
        
        vendor.setOnboardingStatus(com.stillfresh.app.sharedentities.enums.OnboardingStatus.BANKING_SETUP);
        vendorRepository.save(vendor);
        clearVendorCache(vendor.getId(), vendor.getEmail());
        
        logger.info("Banking model set to {} for vendor {}", request.getBankingModel(), vendor.getEmail());
    }
    
    /**
     * Step 4: Setup payment account
     * Initializes payment account based on country and banking model
     */
    public void setupPaymentAccount() throws IOException {
        Vendor vendor = getVendorFromContext();
        
        // Validate onboarding status
        if (vendor.getOnboardingStatus() != com.stillfresh.app.sharedentities.enums.OnboardingStatus.BANKING_SETUP) {
            throw new RuntimeException("Banking model must be set before setting up payment account. Current status: " + vendor.getOnboardingStatus());
        }
        
        // For SHARED banking: only HQ should setup payment account
        if (Boolean.TRUE.equals(vendor.getUsesSharedPaymentAccount())) {
            if (!Boolean.TRUE.equals(vendor.getIsHeadquarters())) {
                throw new RuntimeException("Only headquarters can setup payment account for SHARED banking model.");
            }
        }
        
        // Initialize payment account (this will be called during verification for existing flow)
        // For new onboarding, we need to ensure country is set
        if (vendor.getCountry() == null || vendor.getCountry().isEmpty()) {
            throw new RuntimeException("Country must be set before setting up payment account. Please update vendor profile with country information.");
        }
        
        // Initialize payment account
        initializeVendorPaymentAccount(vendor);
        
        vendor.setOnboardingStatus(com.stillfresh.app.sharedentities.enums.OnboardingStatus.PAYMENT_CONFIGURED);
        vendorRepository.save(vendor);
        clearVendorCache(vendor.getId(), vendor.getEmail());
        
        logger.info("Payment account setup completed for vendor {}", vendor.getEmail());
    }
    
    /**
     * Step 5: Complete onboarding
     * Marks onboarding as complete, enabling full platform functionality
     */
    public void completeOnboarding() {
        Vendor vendor = getVendorFromContext();
        
        // Validate onboarding status
        if (vendor.getOnboardingStatus() != com.stillfresh.app.sharedentities.enums.OnboardingStatus.PAYMENT_CONFIGURED) {
            throw new RuntimeException("Payment account must be configured before completing onboarding. Current status: " + vendor.getOnboardingStatus());
        }
        
        vendor.setOnboardingStatus(com.stillfresh.app.sharedentities.enums.OnboardingStatus.COMPLETED);
        vendorRepository.save(vendor);
        clearVendorCache(vendor.getId(), vendor.getEmail());
        
        logger.info("Onboarding completed for vendor {}", vendor.getEmail());
    }
    
    /**
     * Get current onboarding status
     */
    public com.stillfresh.app.sharedentities.enums.OnboardingStatus getOnboardingStatus() {
        Vendor vendor = getVendorFromContext();
        com.stillfresh.app.sharedentities.enums.OnboardingStatus status = vendor.getOnboardingStatus();
        
        // If status is null, determine default based on vendor state
        if (status == null) {
            // If vendor is active and has been verified, default to VERIFIED
            if (vendor.getStatus() == Status.ACTIVE) {
                status = com.stillfresh.app.sharedentities.enums.OnboardingStatus.VERIFIED;
                vendor.setOnboardingStatus(status);
                vendorRepository.save(vendor);
                clearVendorCache(vendor.getId(), vendor.getEmail());
                logger.info("Set default onboarding status VERIFIED for vendor {} (status was null)", vendor.getEmail());
            } else {
                // If vendor is inactive, default to PENDING_VERIFICATION
                status = com.stillfresh.app.sharedentities.enums.OnboardingStatus.PENDING_VERIFICATION;
            }
        }

        // SHARED chain locations (especially branches created while the chain was still
        // INDIVIDUAL) can stay parked at BANKING_SETUP even after HQ already has a payout
        // destination. Once money routes through HQ, this location needs no bank form —
        // advance them so login does not ask for per-location bank details.
        if (status != com.stillfresh.app.sharedentities.enums.OnboardingStatus.COMPLETED
                && Boolean.TRUE.equals(vendor.getUsesSharedPaymentAccount())
                && hasPayoutDestination(vendor)
                && (status == com.stillfresh.app.sharedentities.enums.OnboardingStatus.BANKING_SETUP
                    || status == com.stillfresh.app.sharedentities.enums.OnboardingStatus.PAYMENT_CONFIGURED)) {
            status = com.stillfresh.app.sharedentities.enums.OnboardingStatus.COMPLETED;
            vendor.setOnboardingStatus(status);
            vendorRepository.save(vendor);
            clearVendorCache(vendor.getId(), vendor.getEmail());
            logger.info("Auto-completed onboarding for SHARED vendor {} — payout destination resolves via headquarters",
                       vendor.getEmail());
        }
        
        return status;
    }
    
    // ========== Chain Location Management Methods ==========
    
    /**
     * Builds the offer-facing vendor snapshot. The event takes primitives, so a vendor that has not
     * filled in coordinates or has no ratings yet would otherwise throw on unboxing.
     */
    private OfferRelatedVendorDetailsEvent buildOfferVendorDetailsEvent(Vendor vendor) {
        return new OfferRelatedVendorDetailsEvent(
            vendor.getId(),
            vendor.getLocationName(),
            vendor.getChainName(),
            vendor.getWebsite(),
            vendor.getImageUrl(),
            vendor.getAddress(),
            vendor.getZipCode(),
            vendor.getLatitude() != null ? vendor.getLatitude() : 0.0,
            vendor.getLongitude() != null ? vendor.getLongitude() : 0.0,
            vendor.getBusinessType(),
            vendor.getReviewsCount(),
            vendor.getAverageRating(),
            vendor.getCountry());
    }
    
    /**
     * True for rows that represent a selling location (headquarters or branch) rather than a
     * worker. Workers are stored in the same table and carry the chain id of their location, so
     * every chain-wide query has to exclude them explicitly.
     */
    private boolean isLocationRow(Vendor vendor) {
        return vendor.getAssignedLocationId() == null;
    }
    
    /**
     * All selling locations of a chain, workers excluded.
     */
    private List<Vendor> findChainLocationRows(String chainId) {
        return vendorRepository.findByChainId(chainId).stream()
            .filter(this::isLocationRow)
            .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * Rejects callers that are not part of a usable chain, before any chain field is dereferenced.
     */
    private void requireChainMembership(Vendor vendor) {
        if (!Boolean.TRUE.equals(vendor.getIsChainLocation())) {
            throw new RuntimeException("Vendor is not part of a chain");
        }
        if (vendor.getChainId() == null || vendor.getChainId().isEmpty()) {
            throw new RuntimeException("Chain ID is missing. Please complete chain setup first.");
        }
    }
    
    /**
     * Loads a location and verifies it belongs to the caller's chain.
     */
    private Vendor requireSameChainLocation(Vendor currentVendor, Long locationId) {
        requireChainMembership(currentVendor);
        Vendor location = findVendorById(locationId);
        if (!currentVendor.getChainId().equals(location.getChainId())) {
            throw new RuntimeException("Cannot access location from different chain. Security violation.");
        }
        if (!isLocationRow(location)) {
            throw new RuntimeException("Target account is a worker, not a location. Use the worker endpoints instead.");
        }
        return location;
    }
    
    /**
     * Chain-wide actions belong to headquarters. A branch admin administers its own location only,
     * which keeps one franchisee from renaming or shutting down its siblings.
     */
    private void requireHeadquarters(Vendor currentVendor, String action) {
        if (currentVendor.getRole() != Role.VENDOR_ADMIN) {
            throw new RuntimeException("Only VENDOR_ADMIN can " + action);
        }
        if (!Boolean.TRUE.equals(currentVendor.getIsHeadquarters())) {
            throw new RuntimeException("Only headquarters can " + action + ". " +
                                      "Please contact your chain headquarters administrator to request this change.");
        }
    }
    
    /**
     * Allows headquarters to act on any location in the chain, and a branch admin only on its own.
     */
    private void requireHeadquartersOrSelf(Vendor currentVendor, Long locationId, String action) {
        if (currentVendor.getRole() != Role.VENDOR_ADMIN) {
            throw new RuntimeException("Only VENDOR_ADMIN can " + action);
        }
        if (!Boolean.TRUE.equals(currentVendor.getIsHeadquarters()) && !currentVendor.getId().equals(locationId)) {
            throw new RuntimeException("Only headquarters can " + action + " for other locations. " +
                                      "You can only " + action + " for your own location.");
        }
    }
    
    /**
     * Builds a username that is free in both this service and authorization-service.
     */
    private String generateUniqueUsername(String email) {
        String base = email.split("@")[0];
        String candidate = base + "_" + System.currentTimeMillis();
        int attempt = 0;
        while (vendorRepository.existsByUsername(candidate) && attempt < 5) {
            candidate = base + "_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 4);
            attempt++;
        }
        if (vendorRepository.existsByUsername(candidate)) {
            throw new RuntimeException("Could not generate a unique username for " + email);
        }
        return candidate;
    }
    
    /**
     * Copies the brand-level profile onto a new location so its offers carry the same business
     * type, opening hours and branding as the rest of the chain. Location-specific fields
     * (address, coordinates, contact) come from the request instead.
     */
    private void inheritChainProfile(Vendor location, Vendor chainVendor) {
        location.setBusinessType(chainVendor.getBusinessType());
        location.setOperatingHours(chainVendor.getOperatingHours());
        location.setSurplusFoodDetails(chainVendor.getSurplusFoodDetails());
        location.setPricingInfo(chainVendor.getPricingInfo());
        location.setEnvironmentalCertifications(chainVendor.getEnvironmentalCertifications());
        location.setWebsite(chainVendor.getWebsite());
        location.setAboutBusiness(chainVendor.getAboutBusiness());
        location.setImageUrl(chainVendor.getImageUrl());
    }
    
    /**
     * Records the legal acceptance that the chain owner made on behalf of accounts they create,
     * so every login account carries an acceptance audit trail.
     */
    private void inheritLegalAcceptance(Vendor account, Vendor acceptedBy) {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        if (acceptedBy.getTermsVersion() != null && !acceptedBy.getTermsVersion().isBlank()) {
            account.setTermsVersion(acceptedBy.getTermsVersion());
            account.setTermsAcceptedAt(now);
        }
        if (acceptedBy.getPrivacyVersion() != null && !acceptedBy.getPrivacyVersion().isBlank()) {
            account.setPrivacyVersion(acceptedBy.getPrivacyVersion());
            account.setPrivacyAcceptedAt(now);
        }
    }
    
    /**
     * Removes an authorization-service account that was created for a vendor row we then failed to
     * persist, so a half-created account does not block the email from being used again.
     */
    private void rollbackAuthorizationAccount(Long globalUserId, String email) {
        try {
            authorizationServiceClient.deleteUser(globalUserId);
            logger.info("Rolled back authorization account {} ({}) after a failed creation", globalUserId, email);
        } catch (Exception e) {
            logger.error("Failed to roll back authorization account {} ({}): {}. It may need manual cleanup.",
                        globalUserId, email, e.getMessage());
        }
    }
    
    /**
     * Adds a new location to the chain
     * Creates a new VENDOR_ADMIN account for the location
     * Links it to the same chain and sets up payment account based on banking model
     * 
     * @return LocationCreationResponse with location details and email status
     */
    @org.springframework.transaction.annotation.Transactional
    public com.stillfresh.app.vendorservice.dto.LocationCreationResponse addChainLocation(com.stillfresh.app.vendorservice.dto.LocationRequest request) {
        Vendor currentVendor = getVendorFromContext();
        
        requireChainMembership(currentVendor);
        requireHeadquarters(currentVendor, "add chain locations");
        
        // Check if email already exists
        if (vendorRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered: " + request.getEmail());
        }
        
        List<Vendor> existingLocations = findChainLocationRows(currentVendor.getChainId());
        if (existingLocations.size() >= maxChainLocations) {
            throw new RuntimeException("This chain already has the maximum of " + maxChainLocations +
                                      " locations. Please contact support to raise the limit.");
        }
        
        String username = generateUniqueUsername(request.getEmail());
        
        // 1. Get global user ID from authorization service
        logger.info("Requesting global user ID for chain location: {}", request.getEmail());
        Map<String, Object> idResponse = authorizationServiceClient.generateUserId(
            new AuthorizationServiceClient.UserIdRequest(
                request.getEmail(), 
                username, 
                Role.VENDOR_ADMIN
            )
        );
        
        if (!(Boolean) idResponse.get("success")) {
            throw new RuntimeException("Failed to generate global user ID: " + idResponse.get("message"));
        }
        
        Long globalUserId = ((Number) idResponse.get("globalUserId")).longValue();
        logger.info("Received global user ID: {} for chain location: {}", globalUserId, request.getEmail());
        
        try {
            return createChainLocation(currentVendor, request, globalUserId, username);
        } catch (RuntimeException e) {
            rollbackAuthorizationAccount(globalUserId, request.getEmail());
            throw e;
        }
    }
    
    /**
     * Persists the location account for {@link #addChainLocation}. Split out so that any failure
     * after the global ID was issued can be compensated by the caller.
     */
    private com.stillfresh.app.vendorservice.dto.LocationCreationResponse createChainLocation(
            Vendor currentVendor,
            com.stillfresh.app.vendorservice.dto.LocationRequest request,
            Long globalUserId,
            String username) {
        // 2. Create new location vendor
        Vendor location = new Vendor();
        location.setId(globalUserId);
        location.setUsername(username);
        location.setEmail(request.getEmail());
        location.setPhone(request.getPhone());
        location.setAddress(request.getAddress());
        location.setZipCode(request.getZipCode());
        location.setLatitude(request.getLatitude());
        location.setLongitude(request.getLongitude());
        
        // Set chain information
        location.setChainId(currentVendor.getChainId());
        location.setChainName(currentVendor.getChainName());
        location.setLocationName(request.getLocationName());
        location.setIsChainLocation(true);
        location.setIsHeadquarters(false);
        location.setIsUniqueVendor(false);
        
        inheritChainProfile(location, currentVendor);
        inheritLegalAcceptance(location, currentVendor);
        
        // Set role and status
        location.setRole(Role.VENDOR_ADMIN);
        location.setStatus(Status.ACTIVE);  // Active immediately (already verified as part of chain)
        
        // Set country (use chain's country if not provided)
        String countryInput = request.getCountry() != null && !request.getCountry().isEmpty() 
            ? request.getCountry() 
            : currentVendor.getCountry();
        
        // Convert country name to ISO 2-letter code
        String countryCode = null;
        if (countryInput != null && !countryInput.isEmpty()) {
            countryCode = countryCodeConverter.convertToIsoCode(countryInput);
            if (countryCode == null) {
                logger.warn("Could not convert country '{}' to ISO code. Using original value.", countryInput);
                countryCode = countryInput.trim().toUpperCase();
            }
        }
        location.setCountry(countryCode);
        
        // Set payment provider based on country code
        if (countryCode != null && !countryCode.isEmpty()) {
            PaymentProvider provider = paymentProviderService.determineProvider(countryCode);
            PayoutModel payoutModel = paymentProviderService.determinePayoutModel(countryCode);
            boolean stripeSupported = paymentProviderService.isStripeSupported(countryCode);
            location.setPaymentProvider(provider);
            location.setPayoutModel(payoutModel);
            location.setStripeSupported(stripeSupported);
            if (countryInput != null && !countryInput.equals(countryCode)) {
                logger.info("Converted country '{}' to ISO code '{}' for chain location", countryInput, countryCode);
            }
        }
        
        // Set banking model based on chain's model
        if (Boolean.TRUE.equals(currentVendor.getUsesSharedPaymentAccount())) {
            // SHARED: Link to headquarters payment account
            Vendor headquarters = findHeadquartersByChainId(currentVendor.getChainId());
            if (headquarters == null) {
                throw new RuntimeException("Headquarters not found for chain. Cannot use shared payment account.");
            }
            location.setUsesSharedPaymentAccount(true);
            location.setSharedPaymentAccountVendorId(headquarters.getId());
        } else {
            // INDIVIDUAL: Each location has its own account
            location.setUsesSharedPaymentAccount(false);
            location.setSharedPaymentAccountVendorId(null);
            // Will setup payment account separately
        }
        
        // Generate temporary password
        String tempPassword = generateSecurePassword();
        location.setPassword(passwordEncoder.encode(tempPassword));
        
        // 3. Save location
        vendorRepository.save(location);
        
        // 4. Update authorization service — without this the location keeps TEMP_PASSWORD and cannot log in
        requireAuthorizationCredentialsSynced(globalUserId, location.getPassword(), Status.ACTIVE);
        
        // 5. Setup payment account if INDIVIDUAL model
        if (!Boolean.TRUE.equals(location.getUsesSharedPaymentAccount())) {
            try {
                initializeVendorPaymentAccount(location);
            } catch (Exception e) {
                logger.warn("Failed to initialize payment account for location {}: {}", location.getEmail(), e.getMessage());
                // Don't fail location creation if payment setup fails
            }
        }
        
        // Onboarding is only complete once there is somewhere to send this location's money.
        // Leaving it at BANKING_SETUP keeps the location from publishing offers it cannot be paid for.
        boolean paymentAccountReady = hasPayoutDestination(location);
        location.setOnboardingStatus(paymentAccountReady
            ? com.stillfresh.app.sharedentities.enums.OnboardingStatus.COMPLETED
            : com.stillfresh.app.sharedentities.enums.OnboardingStatus.BANKING_SETUP);
        vendorRepository.save(location);
        if (!paymentAccountReady) {
            logger.warn("Location {} (ID: {}) has no payout destination yet. Offers stay blocked until a payment account is configured.",
                       location.getLocationName(), location.getId());
        }
        
        // 6. Send credentials email to location contact and track status
        boolean emailSent = false;
        String emailError = null;
        boolean isRecipientError = false;  // True if error is due to invalid recipient/domain
        
        try {
            emailService.sendVendorCredentialsEmail(location.getEmail(), location.getUsername(), tempPassword, loginUrl);
            emailSent = true;
            logger.info("Credentials email sent successfully to location contact: {}", location.getEmail());
        } catch (Exception e) {
            emailSent = false;
            emailError = e.getMessage();
            
            // Check if it's a recipient/domain validation error vs temporary service issue
            String errorMessage = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
            
            // Check for recipient/domain validation errors
            if (errorMessage.contains("invalid recipient") || 
                errorMessage.contains("recipient address rejected") ||
                errorMessage.contains("address does not exist") ||
                errorMessage.contains("mailbox unavailable") ||
                (errorMessage.contains("domain") && (errorMessage.contains("not allowed") || errorMessage.contains("invalid")))) {
                isRecipientError = true;
                logger.error("Email recipient validation failed for location {}: {}. This may indicate an invalid email address.", 
                           location.getEmail(), e.getMessage());
            } else if (errorMessage.contains("not allowed to send") && errorMessage.contains("free accounts")) {
                // This is a Mailgun sandbox restriction - email might be valid but can't be sent due to account limits
                isRecipientError = false;  // Not a recipient error, but a service limitation
                logger.warn("Email sending restricted by Mailgun (free account limitations) for location {}: {}. " +
                           "Email format is valid but cannot be sent. Consider upgrading Mailgun account or adding recipient to authorized list.", 
                           location.getEmail(), e.getMessage());
            } else {
                // Temporary service error
                logger.warn("Temporary email service error for location {}: {}. Location was created successfully.", 
                           location.getEmail(), e.getMessage());
            }
        }
        
        clearVendorCache(location.getId(), location.getEmail());
        logger.info("Chain location added: {} (ID: {}) to chain {}", request.getLocationName(), globalUserId, currentVendor.getChainName());
        
        // 7. Build response with email status
        com.stillfresh.app.vendorservice.dto.LocationCreationResponse response = 
            new com.stillfresh.app.vendorservice.dto.LocationCreationResponse();
        response.setLocationId(location.getId());
        response.setLocationName(location.getLocationName());
        response.setEmail(location.getEmail());
        response.setEmailSent(emailSent);
        // Do not return raw Mailgun/provider error text to API clients
        response.setEmailError(emailSent ? null : (isRecipientError ? "invalid_recipient" : "email_delivery_failed"));
        response.setPaymentAccountReady(paymentAccountReady);
        
        String paymentNotice = paymentAccountReady ? "" :
            " This location still needs a payout account before it can publish offers - " +
            "use the location payment setup endpoint to finish it.";
        
        if (emailSent) {
            response.setMessage("Location added successfully. Credentials sent to: " + location.getEmail() + paymentNotice);
        } else {
            // Never return plaintext passwords in API responses — use password-reset to recover access
            response.setUsername(location.getUsername());

            if (isRecipientError) {
                response.setMessage("Location was created, but credentials email could not be sent due to an invalid recipient or domain. " +
                                  "Verify the email address and trigger a password reset for username '" +
                                  location.getUsername() + "'." + paymentNotice);
            } else {
                response.setMessage("Location added successfully, but credentials email could not be sent. " +
                                  "Trigger a password reset for username '" + location.getUsername() +
                                  "' to deliver login credentials securely." + paymentNotice);
            }
        }
        
        return response;
    }
    
    /**
     * Gets all locations in the current vendor's chain
     * Security: Only returns locations from the same chain
     */
    public List<Vendor> getChainLocations() {
        Vendor currentVendor = getVendorFromContext();
        
        requireChainMembership(currentVendor);
        
        return findChainLocationRows(currentVendor.getChainId());
    }

    /**
     * Sales statistics for all selling locations in a chain.
     * HQ VENDOR_ADMIN: chain derived from authenticated vendor.
     * SUPER_ADMIN: requires {@code chainId} query param.
     */
    public ChainLocationStatsResponse getChainLocationStats(OffsetDateTime from, OffsetDateTime to, String chainIdParam) {
        String chainId;
        String chainName;
        List<Vendor> locations;

        if (isSuperAdmin()) {
            if (chainIdParam == null || chainIdParam.isBlank()) {
                throw new IllegalArgumentException("chainId is required for SUPER_ADMIN");
            }
            chainId = chainIdParam.trim();
            locations = findChainLocationRows(chainId);
            if (locations.isEmpty()) {
                throw new IllegalArgumentException("No locations found for chain: " + chainId);
            }
            chainName = locations.stream()
                    .map(Vendor::getChainName)
                    .filter(name -> name != null && !name.isBlank())
                    .findFirst()
                    .orElse(null);
        } else {
            Vendor currentVendor = getVendorFromContext();
            requireChainMembership(currentVendor);
            requireHeadquarters(currentVendor, "view chain stats");
            chainId = currentVendor.getChainId();
            chainName = currentVendor.getChainName();
            locations = findChainLocationRows(chainId);
        }

        String fromStr = from != null ? from.toString() : null;
        String toStr = to != null ? to.toString() : null;

        ChainLocationStatsResponse response = new ChainLocationStatsResponse();
        response.setChainId(chainId);
        response.setChainName(chainName);
        response.setFrom(from);
        response.setTo(to);

        List<ChainLocationStatsResponse.LocationStatsEntry> entries = new ArrayList<>();
        long totalUnits = 0L;
        long totalVendorEarnings = 0L;
        long totalPlatformFee = 0L;
        long totalGross = 0L;

        for (Vendor location : locations) {
            ChainLocationStatsResponse.LocationStatsEntry entry = new ChainLocationStatsResponse.LocationStatsEntry();
            entry.setVendorId(location.getId());
            entry.setLocationName(location.getLocationName());
            entry.setIsHeadquarters(location.getIsHeadquarters());
            try {
                VendorStatsResponse stats = orderClient.getVendorStats(location.getId(), fromStr, toStr, null);
                entry.setStats(stats);
                if (stats != null) {
                    totalUnits += stats.getTotalUnitsSold();
                    totalVendorEarnings += stats.getTotalVendorEarningsCents();
                    totalPlatformFee += stats.getTotalPlatformFeeCents();
                    totalGross += stats.getTotalGrossRevenueCents();
                }
            } catch (Exception e) {
                logger.warn("Chain stats unavailable for location {} ({}): {}",
                        location.getId(), location.getLocationName(), e.getMessage());
                entry.setError(e.getMessage() != null ? e.getMessage() : "Failed to load stats");
            }
            entries.add(entry);
        }

        response.setLocations(entries);
        response.setChainTotals(new VendorStatsResponse(
                totalUnits,
                totalVendorEarnings,
                totalPlatformFee,
                totalGross,
                List.of(),
                from,
                to
        ));
        return response;
    }

    private boolean isSuperAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return false;
        }
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_SUPER_ADMIN"::equals);
    }
    
    /**
     * Updates a chain location
     * Security: Headquarters can update any branch; a branch admin only its own location
     */
    @org.springframework.transaction.annotation.Transactional
    public void updateChainLocation(Long locationId, com.stillfresh.app.vendorservice.dto.LocationRequest request) {
        Vendor currentVendor = getVendorFromContext();
        Vendor location = requireSameChainLocation(currentVendor, locationId);
        
        requireHeadquartersOrSelf(currentVendor, locationId, "update locations");
        
        // Prevent updating headquarters (should use separate endpoint if needed)
        if (Boolean.TRUE.equals(location.getIsHeadquarters())) {
            throw new RuntimeException("Cannot update headquarters using this endpoint. Use profile update instead.");
        }
        
        // Update location details
        location.setLocationName(request.getLocationName());
        location.setPhone(request.getPhone());
        location.setAddress(request.getAddress());
        location.setZipCode(request.getZipCode());
        location.setLatitude(request.getLatitude());
        location.setLongitude(request.getLongitude());
        
        if (request.getCountry() != null && !request.getCountry().isEmpty()) {
            // Convert country name to ISO 2-letter code
            String countryCode = countryCodeConverter.convertToIsoCode(request.getCountry());
            if (countryCode == null) {
                logger.warn("Could not convert country '{}' to ISO code. Using original value.", request.getCountry());
                countryCode = request.getCountry().trim().toUpperCase();
            }
            location.setCountry(countryCode);
            if (!request.getCountry().equals(countryCode)) {
                logger.info("Converted country '{}' to ISO code '{}' when updating chain location", request.getCountry(), countryCode);
            }
        }
        
        vendorRepository.save(location);
        clearVendorCache(location.getId(), location.getEmail());
        
        // Offers keep a snapshot of the location's name, address and coordinates, so they have to
        // be refreshed or customers keep seeing the old details.
        eventPublisher.publishOfferRelatedVendorDetailsEvent(buildOfferVendorDetailsEvent(location));
        
        logger.info("Chain location updated: {} (ID: {})", location.getLocationName(), locationId);
    }
    
    /**
     * Removes a chain location (deactivates it)
     * Security: Headquarters only, and never headquarters itself
     */
    @org.springframework.transaction.annotation.Transactional
    public void removeChainLocation(Long locationId) {
        Vendor currentVendor = getVendorFromContext();
        Vendor location = requireSameChainLocation(currentVendor, locationId);
        
        requireHeadquarters(currentVendor, "remove chain locations");
        
        // Prevent removing headquarters
        if (Boolean.TRUE.equals(location.getIsHeadquarters())) {
            throw new RuntimeException("Cannot remove headquarters location. Please contact support.");
        }
        
        // Prevent removing self
        if (location.getId().equals(currentVendor.getId())) {
            throw new RuntimeException("Cannot remove your own location. Please contact support.");
        }
        
        // Deactivate location
        location.setStatus(Status.INACTIVE);
        
        // Invalidate all offers for this location
        try {
            eventPublisher.invalidateAllOffers(new AllOffersInvalidationEvent(location.getId()));
        } catch (Exception e) {
            logger.warn("Failed to invalidate offers for location {}: {}", locationId, e.getMessage());
        }
        
        vendorRepository.save(location);
        clearVendorCache(location.getId(), location.getEmail());
        syncStatusToAuthorizationService(location);
        
        // Workers publish offers on behalf of their location, so leaving them active would let a
        // removed location keep selling.
        List<Vendor> workers = vendorRepository.findByAssignedLocationId(location.getId());
        for (Vendor worker : workers) {
            if (worker.getStatus() == Status.INACTIVE) {
                continue;
            }
            worker.setStatus(Status.INACTIVE);
            vendorRepository.save(worker);
            clearVendorCache(worker.getId(), worker.getEmail());
            syncStatusToAuthorizationService(worker);
            logger.info("Deactivated worker {} (ID: {}) along with its location {}", worker.getEmail(), worker.getId(), locationId);
        }
        
        logger.info("Chain location removed (deactivated): {} (ID: {}), workers deactivated: {}",
                   location.getLocationName(), locationId, workers.size());
    }
    
    /**
     * Helper method to find headquarters by chain ID
     */
    private Vendor findHeadquartersByChainId(String chainId) {
        List<Vendor> chainLocations = vendorRepository.findByChainId(chainId);
        return chainLocations.stream()
            .filter(loc -> Boolean.TRUE.equals(loc.getIsHeadquarters()))
            .findFirst()
            .orElse(null);
    }
    
    // ========== Banking Model Management Methods ==========
    
    /**
     * Switches banking model for the entire chain
     * SHARED: All locations use headquarters payment account
     * INDIVIDUAL: Each location must have its own payment account
     * 
     * Sends real-time notifications to all chain locations via:
     * 1. Email notifications to location managers
     * 2. Kafka event for in-app/push notifications
     * 3. Audit logging
     */
    @org.springframework.transaction.annotation.Transactional
    public void switchBankingModel(com.stillfresh.app.vendorservice.dto.SwitchBankingModelRequest request) throws IOException {
        Vendor currentVendor = getVendorFromContext();
        
        // Security: Only VENDOR_ADMIN can switch banking model
        if (currentVendor.getRole() != Role.VENDOR_ADMIN) {
            throw new RuntimeException("Only VENDOR_ADMIN can switch banking model");
        }
        
        // Only chains can switch banking models
        if (!Boolean.TRUE.equals(currentVendor.getIsChainLocation())) {
            throw new RuntimeException("Only chain vendors can switch banking models. Unique vendors use INDIVIDUAL model.");
        }
        
        if (currentVendor.getChainId() == null || currentVendor.getChainId().isEmpty()) {
            throw new RuntimeException("Chain ID is missing");
        }
        
        // Worker rows carry a copy of their location's banking flags, so they are updated too, but
        // only real locations are counted, notified and have their offers invalidated.
        List<Vendor> chainRows = vendorRepository.findByChainId(currentVendor.getChainId());
        List<Vendor> chainLocations = chainRows.stream()
            .filter(this::isLocationRow)
            .collect(java.util.stream.Collectors.toList());
        Vendor headquarters = findHeadquartersByChainId(currentVendor.getChainId());
        
        if (headquarters == null) {
            throw new RuntimeException("Headquarters not found for chain");
        }
        
        // Security: Only Headquarters VENDOR_ADMIN can switch banking model
        // Non-headquarters locations must follow instructions from headquarters
        if (!currentVendor.getId().equals(headquarters.getId())) {
            throw new RuntimeException("Only Headquarters VENDOR_ADMIN can switch banking model. " +
                                      "Please contact your chain headquarters administrator to request this change.");
        }
        
        // Determine previous banking model (before change)
        String previousBankingModel = Boolean.TRUE.equals(headquarters.getUsesSharedPaymentAccount()) 
            ? "SHARED" : "INDIVIDUAL";
        String newBankingModel = request.getBankingModel() == 
            com.stillfresh.app.vendorservice.dto.SwitchBankingModelRequest.BankingModel.SHARED 
            ? "SHARED" : "INDIVIDUAL";
        
        // Skip if no actual change
        if (previousBankingModel.equals(newBankingModel)) {
            logger.info("Banking model is already set to {} for chain: {}. No change needed.", 
                       newBankingModel, currentVendor.getChainName());
            return;
        }
        
        if (request.getBankingModel() == com.stillfresh.app.vendorservice.dto.SwitchBankingModelRequest.BankingModel.SHARED) {
            // Switching to SHARED routes every location's money through headquarters, so
            // headquarters must be payable first - on either rail.
            if (!hasPayoutDestination(headquarters)) {
                throw new RuntimeException("Headquarters must have a payment account before switching to SHARED model. Please setup payment account for headquarters first.");
            }
            
            // Update all locations to use shared account
            for (Vendor row : chainRows) {
                row.setUsesSharedPaymentAccount(true);
                row.setSharedPaymentAccountVendorId(headquarters.getId());
                // Branches no longer need their own bank details — complete onboarding when HQ can be paid.
                if (isLocationRow(row)
                        && row.getOnboardingStatus() != com.stillfresh.app.sharedentities.enums.OnboardingStatus.COMPLETED
                        && hasPayoutDestination(row)) {
                    row.setOnboardingStatus(com.stillfresh.app.sharedentities.enums.OnboardingStatus.COMPLETED);
                }
                vendorRepository.save(row);
                clearVendorCache(row.getId(), row.getEmail());
            }
            
            logger.info("Banking model switched to SHARED for chain: {}", currentVendor.getChainName());
            
        } else {
            // Switching to INDIVIDUAL: Each location needs its own account
            // Note: When switching from SHARED to INDIVIDUAL, locations may not have individual accounts yet.
            // This is expected - they will need to set up individual accounts before reactivating offers.
            // All offers are invalidated during this switch to ensure locations set up accounts first.
            
            // Update all locations to use individual accounts
            for (Vendor row : chainRows) {
                row.setUsesSharedPaymentAccount(false);
                row.setSharedPaymentAccountVendorId(null);
                // After leaving SHARED, a branch without its own payout destination must set one up again.
                if (isLocationRow(row)
                        && !Boolean.TRUE.equals(row.getIsHeadquarters())
                        && !hasPayoutDestination(row)
                        && row.getOnboardingStatus() == com.stillfresh.app.sharedentities.enums.OnboardingStatus.COMPLETED) {
                    row.setOnboardingStatus(com.stillfresh.app.sharedentities.enums.OnboardingStatus.BANKING_SETUP);
                }
                vendorRepository.save(row);
                clearVendorCache(row.getId(), row.getEmail());
            }
            
            // Invalidate all offers from all chain locations when switching to INDIVIDUAL
            // Reason: Each location must set up their own payment account before offers can be active
            logger.info("Invalidating all offers for chain locations due to INDIVIDUAL banking model switch");
            int invalidatedOffersCount = 0;
            for (Vendor location : chainLocations) {
                try {
                    eventPublisher.invalidateAllOffers(new AllOffersInvalidationEvent(location.getId()));
                    invalidatedOffersCount++;
                    logger.info("Invalidated all offers for location: {} (ID: {})", 
                               location.getLocationName(), location.getId());
                } catch (Exception e) {
                    logger.warn("Failed to invalidate offers for location {} ({}): {}. " +
                               "Banking model change succeeded, but offers may need manual invalidation.",
                               location.getLocationName(), location.getId(), e.getMessage());
                }
            }
            logger.info("Banking model switched to INDIVIDUAL for chain: {} (invalidated offers for {} locations)", 
                       currentVendor.getChainName(), invalidatedOffersCount);
        }
        
        // ========== NOTIFICATION SYSTEM ==========
        // 1. Publish Kafka event for real-time in-app/push notifications
        // Collect all location vendor IDs for notification
        java.util.List<Long> locationIds = chainLocations.stream()
            .map(Vendor::getId)
            .collect(java.util.stream.Collectors.toList());
        
        BankingModelChangedEvent bankingEvent = new BankingModelChangedEvent(
            currentVendor.getChainId(),
            currentVendor.getChainName(),
            newBankingModel,
            previousBankingModel,
            currentVendor.getId(),
            currentVendor.getEmail(),
            headquarters.getId(),
            headquarters.getEmail(),
            locationIds
        );
        eventPublisher.publishBankingModelChangedEvent(bankingEvent);
        logger.info("Published BankingModelChangedEvent for chain: {} ({} locations)", 
                   currentVendor.getChainName(), locationIds.size());
        
        // 2. Send email notifications to all location managers (non-blocking)
        String emailSubject = String.format("Banking Model Changed - %s", currentVendor.getChainName());
        String emailBody;
        
        if ("SHARED".equals(newBankingModel)) {
            emailBody = String.format(
                "Dear %s Location Manager,\n\n" +
                "The banking model for your chain '%s' has been changed to SHARED.\n\n" +
                "What this means:\n" +
                "- All locations in your chain will now use the headquarters payment account.\n" +
                "- Payments from all locations will be routed to the headquarters Stripe account.\n" +
                "- You no longer need to manage individual payment accounts for this location.\n\n" +
                "Changed by: %s (%s)\n" +
                "Headquarters: %s (%s)\n" +
                "Effective immediately.\n\n" +
                "If you have any questions, please contact your chain administrator.\n\n" +
                "Best regards,\n" +
                "StillFresh Team",
                currentVendor.getChainName(),
                currentVendor.getChainName(),
                currentVendor.getEmail(),
                currentVendor.getEmail(),
                headquarters.getLocationName() != null ? headquarters.getLocationName() : "Headquarters",
                headquarters.getEmail()
            );
        } else {
            emailBody = String.format(
                "Dear %s Location Manager,\n\n" +
                "The banking model for your chain '%s' has been changed to INDIVIDUAL.\n\n" +
                "What this means:\n" +
                "- Each location now uses its own payment account.\n" +
                "- Payments from your location will be routed to your individual Stripe account.\n" +
                "- You are responsible for managing your own payment account.\n\n" +
                "IMPORTANT - OFFER INVALIDATION:\n" +
                "All active offers for your location have been automatically invalidated.\n" +
                "This is required because your location must have its own payment account configured\n" +
                "before offers can be active. Once you have set up your individual payment account,\n" +
                "you can reactivate your offers through the vendor dashboard.\n\n" +
                "Next Steps:\n" +
                "1. Set up your individual payment account (if not already done)\n" +
                "2. Verify your payment account is ready to receive payments\n" +
                "3. Reactivate your offers through the vendor dashboard\n\n" +
                "Changed by: %s (%s)\n" +
                "Effective immediately.\n\n" +
                "If you have any questions, please contact your chain administrator.\n\n" +
                "Best regards,\n" +
                "StillFresh Team",
                currentVendor.getChainName(),
                currentVendor.getChainName(),
                currentVendor.getEmail(),
                currentVendor.getEmail()
            );
        }
        
        // Send emails to all locations (non-blocking - don't fail if email fails)
        for (Vendor location : chainLocations) {
            // Skip sending email to the person who made the change (they already know)
            if (!location.getId().equals(currentVendor.getId())) {
                try {
                    emailService.sendEmail(location.getEmail(), emailSubject, emailBody);
                    logger.info("Banking model change notification email sent to location: {} ({})", 
                               location.getLocationName(), location.getEmail());
                } catch (Exception e) {
                    logger.warn("Failed to send banking model change email to location {} ({}): {}. " +
                               "Change was successful, but notification email failed.",
                               location.getLocationName(), location.getEmail(), e.getMessage());
                    // Don't fail the operation if email fails
                }
            }
        }
        
        logger.info("Banking model switch completed and notifications sent for chain: {} ({} -> {})", 
                   currentVendor.getChainName(), previousBankingModel, newBankingModel);
    }
    
    /**
     * Sets up individual payment account for a specific location
     * Only used when banking model is INDIVIDUAL
     */
    @org.springframework.transaction.annotation.Transactional
    public void setupLocationPaymentAccount(Long locationId) throws IOException {
        Vendor currentVendor = getVendorFromContext();
        Vendor location = requireSameChainLocation(currentVendor, locationId);
        
        requireHeadquartersOrSelf(currentVendor, locationId, "setup payment accounts");
        
        // Validate location is part of a chain
        if (!Boolean.TRUE.equals(location.getIsChainLocation())) {
            throw new RuntimeException("Location is not part of a chain");
        }
        
        // Validate location uses individual banking model
        if (Boolean.TRUE.equals(location.getUsesSharedPaymentAccount())) {
            throw new RuntimeException("Location uses shared payment account. Cannot setup individual account. Switch banking model first.");
        }
        
        // Validate country is set
        if (location.getCountry() == null || location.getCountry().isEmpty()) {
            throw new RuntimeException("Country must be set before setting up payment account");
        }
        
        // Initialize payment account
        initializeVendorPaymentAccount(location);
        
        // A location created before its payout account existed is parked at BANKING_SETUP; finishing
        // the account here is what completes its onboarding.
        if (hasPayoutDestination(location)
                && location.getOnboardingStatus() != com.stillfresh.app.sharedentities.enums.OnboardingStatus.COMPLETED) {
            location.setOnboardingStatus(com.stillfresh.app.sharedentities.enums.OnboardingStatus.COMPLETED);
        }
        
        vendorRepository.save(location);
        clearVendorCache(location.getId(), location.getEmail());
        
        logger.info("Payment account setup completed for location: {} (ID: {})", location.getLocationName(), locationId);
    }

    /**
     * Returns masked MoR bank details for a chain location.
     * Headquarters may read any location in the chain; a branch admin only its own.
     * On SHARED banking every location is paid through headquarters — use the self
     * {@code GET /vendors/mor/bank-details} endpoint on the HQ account instead.
     */
    public com.stillfresh.app.vendorservice.dto.MorBankDetailsResponse getMoRBankDetailsForLocation(Long locationId) {
        Vendor currentVendor = getVendorFromContext();
        Vendor location = requireSameChainLocation(currentVendor, locationId);
        requireHeadquartersOrSelf(currentVendor, locationId, "view bank details");

        if (Boolean.TRUE.equals(location.getUsesSharedPaymentAccount())) {
            throw new RuntimeException(
                "Location uses shared payment account. Bank details are managed on the headquarters account.");
        }
        if (location.getPayoutModel() != PayoutModel.MOR) {
            throw new RuntimeException("Vendor is not using MoR model");
        }
        return toMoRBankDetailsResponse(location);
    }

    /**
     * Writes MoR bank details onto a chain location (INDIVIDUAL banking model).
     * Headquarters may update any location in the chain; a branch admin only its own.
     * Same partial-update and validation rules as {@link #submitBankDetails(Map)}.
     * Security email about destination changes goes to the <em>location's</em> email.
     */
    @org.springframework.transaction.annotation.Transactional
    public void submitBankDetailsForLocation(Long locationId, Map<String, String> bankDetails) {
        Vendor currentVendor = getVendorFromContext();
        Vendor location = requireSameChainLocation(currentVendor, locationId);
        requireHeadquartersOrSelf(currentVendor, locationId, "update bank details");

        if (!Boolean.TRUE.equals(location.getIsChainLocation())) {
            throw new RuntimeException("Location is not part of a chain");
        }
        if (Boolean.TRUE.equals(location.getUsesSharedPaymentAccount())) {
            throw new RuntimeException(
                "Location uses shared payment account. Cannot set individual bank details. Switch banking model first.");
        }
        if (location.getCountry() == null || location.getCountry().isEmpty()) {
            throw new RuntimeException("Country must be set before setting up payment account");
        }

        // Ensure MoR payout model is initialised (same as setup-payment-account) so a HQ admin
        // can finish bank details in one step without a prior empty shell call.
        if (location.getPayoutModel() != PayoutModel.MOR) {
            initializeVendorPaymentAccount(location);
            location = findVendorById(locationId);
        }
        if (location.getPayoutModel() != PayoutModel.MOR) {
            throw new RuntimeException("Vendor is not using MoR model");
        }

        applyBankDetails(location, bankDetails);

        if (hasPayoutDestination(location)
                && location.getOnboardingStatus() != com.stillfresh.app.sharedentities.enums.OnboardingStatus.COMPLETED) {
            location.setOnboardingStatus(com.stillfresh.app.sharedentities.enums.OnboardingStatus.COMPLETED);
            vendorRepository.save(location);
            clearVendorCache(location.getId(), location.getEmail());
        }

        logger.info("Bank details submitted for chain location {} (ID: {}) by vendor {}",
            location.getLocationName(), locationId, currentVendor.getEmail());
    }
    
    /**
     * Resolves the vendor whose payment account actually receives this vendor's money.
     * On the SHARED banking model that is the account owner (headquarters); otherwise the
     * vendor itself. Falls back to the vendor when the owner row is missing so that a broken
     * link degrades to "pay the location directly" instead of losing the payout entirely.
     */
    public Vendor resolvePayoutAccountOwner(Vendor vendor) {
        if (vendor == null) {
            return null;
        }
        Long ownerId = vendor.getSharedPaymentAccountVendorId();
        if (!Boolean.TRUE.equals(vendor.getUsesSharedPaymentAccount())
                || ownerId == null
                || ownerId.equals(vendor.getId())) {
            return vendor;
        }
        Optional<Vendor> owner = getVendorById(ownerId);
        if (owner.isEmpty()) {
            logger.warn("Vendor {} uses a shared payment account but owner {} was not found. Falling back to the vendor's own account.",
                       vendor.getId(), ownerId);
            return vendor;
        }
        return owner.get();
    }

    /**
     * True when the account that receives this vendor's money can actually be paid out:
     * a Stripe Connect account for CONNECT vendors, bank details for MoR vendors.
     * Deliberately a local check (no provider API call) so it is cheap enough to gate writes.
     */
    public boolean hasPayoutDestination(Vendor vendor) {
        Vendor owner = resolvePayoutAccountOwner(vendor);
        if (owner == null) {
            return false;
        }
        PayoutModel model = owner.getPayoutModel() != null ? owner.getPayoutModel() : vendor.getPayoutModel();
        if (model == PayoutModel.CONNECT) {
            return owner.getStripeAccountId() != null && !owner.getStripeAccountId().isBlank();
        }
        if (model == PayoutModel.MOR) {
            return (owner.getBankAccountNumber() != null && !owner.getBankAccountNumber().isBlank())
                || (owner.getBankIban() != null && !owner.getBankIban().isBlank());
        }
        return false;
    }

    /**
     * Gets banking model information for the chain
     */
    public Map<String, Object> getBankingModelInfo() {
        Vendor currentVendor = getVendorFromContext();
        
        requireChainMembership(currentVendor);
        
        List<Vendor> chainLocations = findChainLocationRows(currentVendor.getChainId());
        Vendor headquarters = findHeadquartersByChainId(currentVendor.getChainId());
        
        Map<String, Object> info = new HashMap<>();
        info.put("bankingModel", Boolean.TRUE.equals(currentVendor.getUsesSharedPaymentAccount()) ? "SHARED" : "INDIVIDUAL");
        info.put("chainName", currentVendor.getChainName());
        info.put("totalLocations", chainLocations.size());
        
        // Count locations that can actually be paid out (on SHARED this resolves to headquarters)
        long locationsWithAccounts = chainLocations.stream()
            .filter(this::hasPayoutDestination)
            .count();
        
        info.put("locationsWithPaymentAccounts", locationsWithAccounts);
        info.put("headquartersHasAccount", headquarters != null && hasPayoutDestination(headquarters));
        
        return info;
    }
    
    // ========== VENDOR Worker Management Methods ==========
    
    /**
     * Creates a VENDOR worker for a specific location
     * Worker can only manage offers for their assigned location
     */
    @org.springframework.transaction.annotation.Transactional
    public Vendor createWorker(Long locationId, com.stillfresh.app.vendorservice.dto.WorkerRequest request) throws IOException {
        Vendor currentVendor = getVendorFromContext();
        Vendor location = findVendorById(locationId);
        
        // Security: Ensure current vendor is VENDOR_ADMIN
        if (currentVendor.getRole() != Role.VENDOR_ADMIN) {
            throw new RuntimeException("Only VENDOR_ADMIN can create workers");
        }
        
        // Security: Ensure location belongs to the same chain (if chain) or is the same vendor (if unique)
        if (Boolean.TRUE.equals(currentVendor.getIsChainLocation())) {
            // Chain: location must be in same chain
            if (!currentVendor.getChainId().equals(location.getChainId())) {
                throw new RuntimeException("Cannot create worker for location from different chain. Security violation.");
            }
            
            // Security: Only headquarters can create workers for other locations
            // Non-headquarters can only create workers for their own location
            if (!Boolean.TRUE.equals(currentVendor.getIsHeadquarters())) {
                if (!currentVendor.getId().equals(locationId)) {
                    throw new RuntimeException("Only headquarters can create workers for other locations. " +
                                              "You can only create workers for your own location.");
                }
            }
        } else {
            // Unique vendor: location must be the same vendor
            if (!currentVendor.getId().equals(location.getId())) {
                throw new RuntimeException("Cannot create worker for different vendor. Security violation.");
            }
        }
        
        // A worker's location must be a real location, not another worker
        if (!isLocationRow(location)) {
            throw new RuntimeException("Target account is a worker, not a location. Workers cannot own workers.");
        }
        
        // Check if email already exists
        if (vendorRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered: " + request.getEmail());
        }
        
        // Usernames are a login identifier here, so a duplicate would break login for both accounts
        if (vendorRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already taken: " + request.getUsername());
        }
        
        long existingWorkers = vendorRepository.findByAssignedLocationId(locationId).size();
        if (existingWorkers >= maxWorkersPerLocation) {
            throw new RuntimeException("This location already has the maximum of " + maxWorkersPerLocation +
                                      " workers. Please contact support to raise the limit.");
        }
        
        // Generate global user ID
        logger.info("Requesting global user ID for worker: {}", request.getEmail());
        Map<String, Object> idResponse = authorizationServiceClient.generateUserId(
            new AuthorizationServiceClient.UserIdRequest(
                request.getEmail(), 
                request.getUsername(), 
                Role.VENDOR
            )
        );
        
        if (!(Boolean) idResponse.get("success")) {
            throw new RuntimeException("Failed to generate global user ID: " + idResponse.get("message"));
        }
        
        Long globalUserId = ((Number) idResponse.get("globalUserId")).longValue();
        logger.info("Received global user ID: {} for worker: {}", globalUserId, request.getEmail());
        
        try {
            return createWorkerAccount(currentVendor, location, request, globalUserId);
        } catch (RuntimeException e) {
            rollbackAuthorizationAccount(globalUserId, request.getEmail());
            throw e;
        }
    }
    
    /**
     * Persists the worker account for {@link #createWorker}. Split out so that any failure after
     * the global ID was issued can be compensated by the caller.
     */
    private Vendor createWorkerAccount(Vendor currentVendor,
                                       Vendor location,
                                       com.stillfresh.app.vendorservice.dto.WorkerRequest request,
                                       Long globalUserId) {
        // Create worker vendor
        Vendor worker = new Vendor();
        worker.setId(globalUserId);
        worker.setUsername(request.getUsername());
        worker.setEmail(request.getEmail());
        worker.setPassword(passwordEncoder.encode(request.getPassword()));
        worker.setPhone(request.getPhone() != null ? request.getPhone() : location.getPhone());
        
        // Set role and status
        worker.setRole(Role.VENDOR);
        worker.setStatus(Status.ACTIVE);
        worker.setOnboardingStatus(com.stillfresh.app.sharedentities.enums.OnboardingStatus.COMPLETED);
        
        // Link worker to location
        worker.setAssignedLocationId(location.getId());
        
        inheritLegalAcceptance(worker, currentVendor);
        
        // Copy location details for worker
        worker.setAddress(location.getAddress());
        worker.setZipCode(location.getZipCode());
        worker.setLatitude(location.getLatitude());
        worker.setLongitude(location.getLongitude());
        worker.setCountry(location.getCountry());
        worker.setBusinessType(location.getBusinessType());
        
        // Set chain information (if location is part of chain)
        if (Boolean.TRUE.equals(location.getIsChainLocation())) {
            worker.setChainId(location.getChainId());
            worker.setChainName(location.getChainName());
            worker.setIsChainLocation(true);
            worker.setIsHeadquarters(false);
            worker.setIsUniqueVendor(false);
            worker.setLocationName(location.getLocationName());
            
            // Set banking model based on location
            worker.setUsesSharedPaymentAccount(location.getUsesSharedPaymentAccount());
            worker.setSharedPaymentAccountVendorId(location.getSharedPaymentAccountVendorId());
        } else {
            worker.setIsChainLocation(false);
            worker.setIsUniqueVendor(true);
            worker.setIsHeadquarters(false);
        }
        
        // Set payment provider (same as location)
        worker.setPaymentProvider(location.getPaymentProvider());
        worker.setPayoutModel(location.getPayoutModel());
        worker.setStripeSupported(location.getStripeSupported());
        
        // Save worker
        vendorRepository.save(worker);
        
        // Without this the auth row keeps TEMP_PASSWORD / INACTIVE and login with the chosen
        // password fails even though the worker exists in both databases.
        requireAuthorizationCredentialsSynced(globalUserId, worker.getPassword(), Status.ACTIVE);
        
        // Send credentials email (non-blocking)
        try {
            emailService.sendVendorCredentialsEmail(worker.getEmail(), worker.getUsername(), request.getPassword(), loginUrl);
            logger.info("Credentials email sent successfully to worker: {}", worker.getEmail());
        } catch (Exception e) {
            logger.warn("Failed to send credentials email to worker {} (id: {}): {}. Worker was created successfully.",
                       worker.getEmail(), worker.getId(), e.getMessage());
            // Don't fail worker creation if email sending fails - worker is already created
        }
        
        clearVendorCache(worker.getId(), worker.getEmail());
        logger.info("Worker created: {} (ID: {}) for location: {}", request.getEmail(), globalUserId, location.getLocationName());
        
        return worker;
    }
    
    /**
     * Headquarters may manage workers of any location in its chain; a branch admin only its own.
     * A standalone vendor may only manage workers of itself.
     */
    private void requireWorkerLocationAccess(Vendor currentVendor, Vendor location, String action) {
        if (currentVendor.getRole() != Role.VENDOR_ADMIN) {
            throw new RuntimeException("Only VENDOR_ADMIN can " + action + " workers");
        }
        if (Boolean.TRUE.equals(currentVendor.getIsChainLocation())) {
            requireChainMembership(currentVendor);
            if (!currentVendor.getChainId().equals(location.getChainId())) {
                throw new RuntimeException("Cannot " + action + " workers for a location from a different chain. Security violation.");
            }
            if (!Boolean.TRUE.equals(currentVendor.getIsHeadquarters()) && !currentVendor.getId().equals(location.getId())) {
                throw new RuntimeException("Only headquarters can " + action + " workers for other locations. " +
                                          "You can only " + action + " workers for your own location.");
            }
        } else if (!currentVendor.getId().equals(location.getId())) {
            throw new RuntimeException("Cannot " + action + " workers for a different vendor. Security violation.");
        }
    }
    
    /**
     * Loads a worker and verifies the caller is allowed to manage it.
     */
    private Vendor requireManageableWorker(Vendor currentVendor, Long workerId, String action) {
        Vendor worker = findVendorById(workerId);
        
        if (worker.getRole() != Role.VENDOR) {
            throw new RuntimeException("Account is not a worker (VENDOR role)");
        }
        if (worker.getAssignedLocationId() == null) {
            throw new RuntimeException("Worker is not assigned to a location");
        }
        
        Vendor location = findVendorById(worker.getAssignedLocationId());
        requireWorkerLocationAccess(currentVendor, location, action);
        
        return worker;
    }
    
    /**
     * Gets all workers for a specific location
     * Security: Only returns workers for locations in the same chain or same vendor
     */
    public List<Vendor> getLocationWorkers(Long locationId) {
        Vendor currentVendor = getVendorFromContext();
        Vendor location = findVendorById(locationId);
        
        requireWorkerLocationAccess(currentVendor, location, "access");
        
        // Get all workers assigned to this location
        List<Vendor> workers = vendorRepository.findByAssignedLocationId(locationId);
        
        // Filter to only VENDOR role (not VENDOR_ADMIN)
        workers = workers.stream()
            .filter(w -> w.getRole() == Role.VENDOR)
            .collect(java.util.stream.Collectors.toList());
        
        return workers;
    }
    
    /**
     * Activates a worker account
     */
    @org.springframework.transaction.annotation.Transactional
    public void activateWorker(Long workerId) {
        Vendor currentVendor = getVendorFromContext();
        Vendor worker = requireManageableWorker(currentVendor, workerId, "activate");
        
        worker.setStatus(Status.ACTIVE);
        vendorRepository.save(worker);
        clearVendorCache(worker.getId(), worker.getEmail());
        syncStatusToAuthorizationService(worker);
        
        logger.info("Worker activated: {} (ID: {})", worker.getEmail(), workerId);
    }
    
    /**
     * Deactivates a worker account
     */
    @org.springframework.transaction.annotation.Transactional
    public void deactivateWorker(Long workerId) {
        Vendor currentVendor = getVendorFromContext();
        Vendor worker = requireManageableWorker(currentVendor, workerId, "deactivate");
        
        worker.setStatus(Status.INACTIVE);
        vendorRepository.save(worker);
        clearVendorCache(worker.getId(), worker.getEmail());
        syncStatusToAuthorizationService(worker);
        
        logger.info("Worker deactivated: {} (ID: {})", worker.getEmail(), workerId);
    }
    
    /**
     * Updates a worker: contact details and, for headquarters, the location they are assigned to.
     */
    @org.springframework.transaction.annotation.Transactional
    public void updateWorker(Long workerId, com.stillfresh.app.vendorservice.dto.WorkerUpdateRequest request) {
        Vendor currentVendor = getVendorFromContext();
        Vendor worker = requireManageableWorker(currentVendor, workerId, "update");
        
        if (request.getUsername() != null && !request.getUsername().isBlank()
                && !request.getUsername().equals(worker.getUsername())) {
            if (vendorRepository.existsByUsername(request.getUsername())) {
                throw new RuntimeException("Username already taken: " + request.getUsername());
            }
            worker.setUsername(request.getUsername());
        }
        
        if (request.getPhone() != null && !request.getPhone().isBlank()) {
            worker.setPhone(request.getPhone());
        }
        
        if (request.getAssignedLocationId() != null
                && !request.getAssignedLocationId().equals(worker.getAssignedLocationId())) {
            Vendor newLocation = findVendorById(request.getAssignedLocationId());
            if (!isLocationRow(newLocation)) {
                throw new RuntimeException("Target account is a worker, not a location.");
            }
            // The caller must be allowed to manage the destination as well, otherwise a branch admin
            // could push a worker into a sibling location.
            requireWorkerLocationAccess(currentVendor, newLocation, "reassign");
            
            worker.setAssignedLocationId(newLocation.getId());
            worker.setLocationName(newLocation.getLocationName());
            worker.setChainId(newLocation.getChainId());
            worker.setChainName(newLocation.getChainName());
            worker.setIsChainLocation(newLocation.getIsChainLocation());
            worker.setAddress(newLocation.getAddress());
            worker.setZipCode(newLocation.getZipCode());
            worker.setLatitude(newLocation.getLatitude());
            worker.setLongitude(newLocation.getLongitude());
            worker.setCountry(newLocation.getCountry());
            worker.setBusinessType(newLocation.getBusinessType());
            worker.setPaymentProvider(newLocation.getPaymentProvider());
            worker.setPayoutModel(newLocation.getPayoutModel());
            worker.setStripeSupported(newLocation.getStripeSupported());
            worker.setUsesSharedPaymentAccount(newLocation.getUsesSharedPaymentAccount());
            worker.setSharedPaymentAccountVendorId(newLocation.getSharedPaymentAccountVendorId());
        }
        
        vendorRepository.save(worker);
        clearVendorCache(worker.getId(), worker.getEmail());
        
        logger.info("Worker updated: {} (ID: {})", worker.getEmail(), workerId);
    }
    
    /**
     * Deletes a worker account
     */
    @org.springframework.transaction.annotation.Transactional
    public void deleteWorker(Long workerId) {
        Vendor currentVendor = getVendorFromContext();
        Vendor worker = requireManageableWorker(currentVendor, workerId, "delete");
        
        // Offers a worker publishes are attributed to their location, not to the worker, so there is
        // nothing to invalidate here - and doing so would take down the whole location's catalogue.
        
        // Delete worker
        vendorRepository.deleteById(workerId);
        clearVendorCache(workerId, worker.getEmail());
        
        // The credentials live in authorization-service. Leaving them behind would keep the login
        // working and permanently reserve the email address.
        try {
            Map<String, Object> response = authorizationServiceClient.deleteUser(workerId);
            if (!Boolean.TRUE.equals(response.get("success"))) {
                logger.error("Failed to delete authorization account for worker {}: {}", workerId, response.get("message"));
            }
        } catch (Exception e) {
            logger.error("Failed to delete authorization account for worker {}: {}. The credentials may still work.",
                        workerId, e.getMessage());
        }
        
        logger.info("Worker deleted: {} (ID: {})", worker.getEmail(), workerId);
    }
    
    // ========== Upgrade Unique Vendor to Chain ==========
    
    /**
     * Rejects a chain name that another chain already uses, so that chain names stay a usable
     * identifier for support and for lookups by name.
     */
    private void requireAvailableChainName(String chainName, String ownChainId) {
        List<Vendor> existing = vendorRepository.findByChainName(chainName);
        boolean takenByAnotherChain = existing.stream()
            .anyMatch(v -> v.getChainId() != null && !v.getChainId().equals(ownChainId));
        if (takenByAnotherChain) {
            throw new RuntimeException("Chain name '" + chainName + "' is already in use. Please choose a different name.");
        }
    }
    
    /**
     * Upgrades a unique vendor to a chain
     * Allows unique vendors to add multiple locations later
     */
    @org.springframework.transaction.annotation.Transactional
    public void upgradeToChain(String chainName) {
        Vendor vendor = getVendorFromContext();
        
        if (Boolean.TRUE.equals(vendor.getIsChainLocation())) {
            throw new RuntimeException("Vendor is already part of a chain");
        }
        
        // Vendors created before the chain fields existed have a null flag; anything that is not
        // already a chain location is a standalone vendor and may upgrade.
        if (Boolean.FALSE.equals(vendor.getIsUniqueVendor()) && vendor.getChainId() != null) {
            throw new RuntimeException("Vendor is already a chain or not a unique vendor");
        }
        
        // Upgrading mid-onboarding would strand the vendor: the chain flow expects the headquarters
        // step, which is no longer reachable once the type has been selected as UNIQUE.
        if (vendor.getOnboardingStatus() != com.stillfresh.app.sharedentities.enums.OnboardingStatus.COMPLETED) {
            throw new RuntimeException("Finish onboarding before upgrading to a chain. Current status: " +
                                      vendor.getOnboardingStatus());
        }
        
        requireAvailableChainName(chainName, null);
        
        // Generate chain ID
        String chainId = UUID.randomUUID().toString();
        
        // Update vendor to chain
        vendor.setChainId(chainId);
        vendor.setChainName(chainName);
        vendor.setIsChainLocation(true);
        vendor.setIsUniqueVendor(false);
        vendor.setIsHeadquarters(true);  // Current location becomes headquarters
        vendor.setLocationName(vendor.getLocationName() != null ? vendor.getLocationName() : vendor.getUsername());
        
        // If banking model was set to INDIVIDUAL, keep it; otherwise default to INDIVIDUAL
        if (vendor.getUsesSharedPaymentAccount() == null) {
            vendor.setUsesSharedPaymentAccount(false);
            vendor.setSharedPaymentAccountVendorId(null);
        }
        
        vendorRepository.save(vendor);
        clearVendorCache(vendor.getId(), vendor.getEmail());
        
        // Offers carry a chain name snapshot, so existing listings need the new brand.
        eventPublisher.publishOfferRelatedVendorDetailsEvent(buildOfferVendorDetailsEvent(vendor));
        
        logger.info("Vendor {} upgraded to chain: {}", vendor.getEmail(), chainName);
    }

    public Vendor registerAdmin(Vendor vendor) throws IOException {
        // Basic validation to prevent duplicate emails or names
        if (vendorRepository.existsByEmail(vendor.getEmail())) {
            throw new RuntimeException("Email is already registered.");
        }

        vendor.setPassword(passwordEncoder.encode(vendor.getPassword()));
        vendor.setRole(Role.ADMIN);  // Assign the ADMIN role
        vendor.setStatus(Status.ACTIVE);

        // Fetch and assign geo-coordinates with fallback
//        try {
//            double[] coordinates = geoLocationService.getCoordinates(vendor.getAddress(), vendor.getZipCode());
//            if (coordinates != null) {
//                vendor.setLatitude(coordinates[0]);
//                vendor.setLongitude(coordinates[1]);
//                logger.info("Successfully set coordinates for admin: {}", vendor.getEmail());
//            } else {
//                logger.warn("Could not geocode address for admin: {}. Setting default coordinates.", vendor.getEmail());
//                vendor.setLatitude(0.0);
//                vendor.setLongitude(0.0);
//            }
//        } catch (Exception e) {
//            logger.error("Geocoding failed for admin: {}. Proceeding with registration without coordinates.", vendor.getEmail(), e);
//            vendor.setLatitude(0.0);
//            vendor.setLongitude(0.0);
//        }

        // Save the admin to the database
        return vendorRepository.save(vendor);
    }

    public Vendor registerVendor(Vendor vendor) throws IOException {
        // Local guard to prevent duplicates even if upstream availability check is skipped/races.
        if (vendorRepository.existsByEmail(vendor.getEmail()) || vendorRepository.existsByUsername(vendor.getUsername())) {
            throw new IllegalStateException("Vendor already exists with this email or username");
        }     
        
        // 1. First, get global user ID from authorization service
        logger.info("Requesting global user ID for vendor: {}", vendor.getEmail());
        Map<String, Object> idResponse = authorizationServiceClient.generateUserId(
            new AuthorizationServiceClient.UserIdRequest(
                vendor.getEmail(), 
                vendor.getUsername(), 
                Role.VENDOR
            )
        );
        
        if (!(Boolean) idResponse.get("success")) {
            throw new RuntimeException("Failed to generate global user ID: " + idResponse.get("message"));
        }
        
        Long globalUserId = ((Number) idResponse.get("globalUserId")).longValue();
        logger.info("Received global user ID: {} for vendor: {}", globalUserId, vendor.getEmail());
        
        // 2. Set the global ID and encode password
        vendor.setId(globalUserId); // Override auto-generation with global ID
        String encodedPassword = passwordEncoder.encode(vendor.getPassword());
        vendor.setPassword(encodedPassword);
        vendor.setRole(Role.VENDOR);
        vendor.setStatus(Status.INACTIVE);  // Inactive until verified

        // Record legal acceptance. Version comes from the client (document displayed);
        // timestamp is stamped server-side.
        java.time.LocalDateTime acceptedAt = java.time.LocalDateTime.now();
        if (vendor.getTermsVersion() != null && !vendor.getTermsVersion().isBlank()) {
            vendor.setTermsAcceptedAt(acceptedAt);
        }
        if (vendor.getPrivacyVersion() != null && !vendor.getPrivacyVersion().isBlank()) {
            vendor.setPrivacyAcceptedAt(acceptedAt);
        }
        
        // Set payment provider and payout model based on country from frontend
        String countryInput = vendor.getCountry();
        if (countryInput != null && !countryInput.isEmpty()) {
            // Convert country name to ISO 2-letter code
            String countryCode = countryCodeConverter.convertToIsoCode(countryInput);
            if (countryCode == null) {
                logger.warn("Could not convert country '{}' to ISO code. Using original value.", countryInput);
                countryCode = countryInput.trim().toUpperCase();
            }
            vendor.setCountry(countryCode);
            PaymentProvider provider = paymentProviderService.determineProvider(countryCode);
            PayoutModel payoutModel = paymentProviderService.determinePayoutModel(countryCode);
            boolean stripeSupported = paymentProviderService.isStripeSupported(countryCode);
            vendor.setPaymentProvider(provider);
            vendor.setPayoutModel(payoutModel);
            vendor.setStripeSupported(stripeSupported);
            if (!countryInput.equals(countryCode)) {
                logger.info("Converted country '{}' to ISO code '{}' for vendor during registration", countryInput, countryCode);
            }
            logger.info("Set payment info for vendor during registration: {} (country: {}, provider: {}, model: {})", 
                       vendor.getEmail(), countryCode, provider, payoutModel);
        } else {
            logger.warn("Country not provided for vendor: {}. Payment account will be initialized during verification.", 
                       vendor.getEmail());
        }
        
        // Fetch and assign geo-coordinates with fallback
//        try {
//            double[] coordinates = geoLocationService.getCoordinates(vendor.getAddress(), vendor.getZipCode());
//            if (coordinates != null) {
//                vendor.setLatitude(coordinates[0]);
//                vendor.setLongitude(coordinates[1]);
//                logger.info("Successfully set coordinates for vendor: {}", vendor.getEmail());
//            } else {
//                logger.warn("Could not geocode address for vendor: {}. Setting default coordinates.", vendor.getEmail());
//                vendor.setLatitude(0.0);
//                vendor.setLongitude(0.0);
//            }
//        } catch (Exception e) {
//            logger.error("Geocoding failed for vendor: {}. Proceeding with registration without coordinates.", vendor.getEmail(), e);
//            vendor.setLatitude(0.0);
//            vendor.setLongitude(0.0);
//        }
        
        // 3. Save vendor with global ID
        vendorRepository.save(vendor);
        
        // 4. Update credentials in authorization service
        logger.info("Updating credentials in authorization service for global user ID: {}", globalUserId);
        try {
            requireAuthorizationCredentialsSynced(globalUserId, encodedPassword, Status.INACTIVE);
        } catch (RuntimeException e) {
            logger.error("Failed to update credentials in authorization service for {}: {}", globalUserId, e.getMessage());
            // Don't throw here as vendor is already saved locally
        }

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

    @org.springframework.transaction.annotation.Transactional
    public boolean verifyVendor(String token) {
        VendorVerificationToken verificationToken = vendorVerificationTokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid token"));
        Vendor vendor = verificationToken.getVendor();
        vendor.setStatus(Status.ACTIVE);
        vendor.setOnboardingStatus(com.stillfresh.app.sharedentities.enums.OnboardingStatus.VERIFIED);

        // A verified business owner administers their own account: they choose the vendor type,
        // manage locations, workers and banking. Workers (which carry an assigned location) are
        // created already active and must stay plain VENDOR.
        boolean promoteToVendorAdmin = vendor.getRole() == Role.VENDOR && vendor.getAssignedLocationId() == null;
        if (promoteToVendorAdmin) {
            vendor.setRole(Role.VENDOR_ADMIN);
        }

        vendorRepository.save(vendor);
        clearVendorCache(vendor.getId(), vendor.getEmail());
        
        // Update authorization service with verification
        logger.info("Verifying vendor in authorization service with global user ID: {}", vendor.getId());
        Map<String, Object> verifyResponse = authorizationServiceClient.verifyUser(vendor.getId());
        
        if (!(Boolean) verifyResponse.get("success")) {
            logger.error("Failed to verify vendor in authorization service: {}", verifyResponse.get("message"));
            // Don't throw exception here as vendor is already verified locally
        }

        // The role lives in the JWT, so it has to be promoted in authorization-service as well or
        // the vendor logs in as VENDOR and every VENDOR_ADMIN endpoint rejects them.
        if (promoteToVendorAdmin) {
            try {
                Map<String, Object> roleResponse = authorizationServiceClient.updateUserRole(vendor.getId(), Role.VENDOR_ADMIN);
                if (!Boolean.TRUE.equals(roleResponse.get("success"))) {
                    logger.error("Failed to promote vendor {} to VENDOR_ADMIN in authorization service: {}",
                                vendor.getId(), roleResponse.get("message"));
                }
            } catch (Exception e) {
                logger.error("Failed to promote vendor {} to VENDOR_ADMIN in authorization service: {}",
                            vendor.getId(), e.getMessage());
            }
        }
        
        // 🔹 Create payment account for vendor when they verify (Stripe Connect or MoR based on country)
        initializeVendorPaymentAccount(vendor);
        
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
        // Hash the new password
        String encodedPassword = passwordEncoder.encode(newPassword);
        vendor.setPassword(encodedPassword);
        vendorRepository.save(vendor);

        // Remove the reset token after successful password reset
        passwordResetTokenRepository.delete(resetToken);
    }
    
    public Vendor findVendorById(Long id) {
        try {
            // Try to get from cache first
            return findVendorByIdFromCache(id);
        } catch (Exception e) {
            logger.warn("Cache retrieval failed for vendor ID {}: {}. Clearing cache and fetching from database.", id, e.getMessage());
            // Clear cache and fetch from database directly
            clearVendorCache(id, null);
            return findVendorByIdFromDatabase(id);
        }
    }
    
    /**
     * Gets vendor by ID, returns Optional
     * @param id Vendor ID
     * @return Optional Vendor
     */
    public Optional<Vendor> getVendorById(Long id) {
        return vendorRepository.findById(id);
    }
    
    /**
     * Gets PaymentProviderService (for internal use)
     * @return PaymentProviderService instance
     */
    public PaymentProviderService getPaymentProviderService() {
        return paymentProviderService;
    }
    
    @Cacheable(value = "vendor", key = "#id", unless = "#result == null")
    public Vendor findVendorByIdFromCache(Long id) {
        Optional<Vendor> vendor = vendorRepository.findById(id);
        logger.info("Finding a vendor from cache {}, with id: {}", vendor.map(Vendor::getUsername).orElse("Not found"), id);
        return vendor.orElseThrow(() -> new RuntimeException("Vendor not found"));
    }
    
    public Vendor findVendorByIdFromDatabase(Long id) {
        Optional<Vendor> vendor = vendorRepository.findById(id);
        logger.info("Finding a vendor from database {}, with id: {}", vendor.map(Vendor::getUsername).orElse("Not found"), id);
        return vendor.orElseThrow(() -> new RuntimeException("Vendor not found"));
    }


    private Date calculateExpiryDate(int hours) {
        Date now = new Date();
        return new Date(now.getTime() + (hours * 60 * 60 * 1000));  // Expiry time in milliseconds
    }
    
    private String extractTokenFromContext() {
        // Try to get token from request attributes (set by GatewayTrustFilter if available)
        // This is a fallback for backward compatibility
        try {
            org.springframework.web.context.request.ServletRequestAttributes attrs = 
                (org.springframework.web.context.request.ServletRequestAttributes) 
                org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                jakarta.servlet.http.HttpServletRequest request = attrs.getRequest();
                String authHeader = request.getHeader("Authorization");
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    return authHeader.substring(7);
                }
            }
        } catch (Exception e) {
            // Fall through to try authentication details
        }
        
        // Fallback: try to get from authentication details (old way)
        try {
            Object details = SecurityContextHolder.getContext().getAuthentication().getDetails();
            if (details instanceof String) {
                String authorizationHeader = (String) details;
                if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
                    return authorizationHeader.substring(7);
                }
            }
        } catch (Exception e) {
            // Ignore
        }
        
        throw new RuntimeException("Unable to extract token from context");
    }

    /**
     * Gets the authenticated vendor from Spring Security context
     * With GatewayTrustFilter, the vendor is already loaded in CustomVendorDetails
     * @return Vendor object
     * @throws RuntimeException if vendor is not authenticated
     */
    public Vendor getVendorFromContext() {
        try {
            // First, try to get vendor directly from CustomVendorDetails (new way with GatewayTrustFilter)
            org.springframework.security.core.Authentication authentication = 
                SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication != null && authentication.getPrincipal() instanceof CustomVendorDetails) {
                CustomVendorDetails vendorDetails = 
                    (CustomVendorDetails) authentication.getPrincipal();
                return vendorDetails.getVendor();
            }
            
            // Fallback: try to extract from token (old way, for backward compatibility)
            return extractVendorFromToken("Bearer " + extractTokenFromContext());
        } catch (Exception e) {
            logger.error("Error getting vendor from context: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get authenticated vendor: " + e.getMessage(), e);
        }
    }

    /**
     * Applies a vendor's self-service profile edit.
     *
     * <p>Takes a {@link VendorProfileUpdateRequest} rather than a {@code Vendor} on purpose: the
     * request must not be able to carry {@code role}, {@code status}, {@code averageRating},
     * {@code reviewsCount}, bank details, onboarding status or payout model. Role and status are
     * changed only through the admin endpoints; ratings are derived from reviews.
     */
    public void updateVendorProfile(VendorProfileUpdateRequest updatedVendor) {
        Vendor currentVendor = getVendorFromContext();
        
        // Track if sensitive data is changed (password, email, username)
        // These changes require re-authentication for security
        boolean sensitiveDataChanged = false;
        
        // Update only non-null fields
        if (updatedVendor.getUsername() != null && !updatedVendor.getUsername().equals(currentVendor.getUsername())) {
            currentVendor.setUsername(updatedVendor.getUsername());
            sensitiveDataChanged = true; // Username is used for authentication
            logger.info("Username changed for vendorId: {}", currentVendor.getId());
        }
        
        // Check if email changed (if email field exists and is being updated)
        if (updatedVendor.getEmail() != null && !updatedVendor.getEmail().equals(currentVendor.getEmail())) {
            currentVendor.setEmail(updatedVendor.getEmail());
            sensitiveDataChanged = true; // Email is used for authentication
            logger.info("Email changed for vendorId: {}", currentVendor.getId());
        }
        
        if (updatedVendor.getAddress() != null) {
            currentVendor.setAddress(updatedVendor.getAddress());
        }
        if (updatedVendor.getPhone() != null) {
            currentVendor.setPhone(updatedVendor.getPhone());
        }
        if (updatedVendor.getPassword() != null) {
            currentVendor.setPassword(passwordEncoder.encode(updatedVendor.getPassword()));
            sensitiveDataChanged = true; // Password change requires re-authentication
            logger.info("Password changed for vendorId: {}", currentVendor.getId());
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
        // averageRating and reviewsCount are derived from customer reviews, not self-reported.
        if (updatedVendor.getImageUrl() != null) {
            currentVendor.setImageUrl(updatedVendor.getImageUrl());
        }
        if (updatedVendor.getWebsite() != null) {
            currentVendor.setWebsite(updatedVendor.getWebsite());
        }
        if (updatedVendor.getAboutBusiness() != null) {
            currentVendor.setAboutBusiness(updatedVendor.getAboutBusiness());
        }
        if (updatedVendor.getContactPerson() != null) {
            currentVendor.setContactPerson(updatedVendor.getContactPerson());
        }
        if (updatedVendor.getZipCode() != null) {
            currentVendor.setZipCode(updatedVendor.getZipCode());
        }
        
        if (updatedVendor.getLongitude() != null && updatedVendor.getLongitude() != currentVendor.getLongitude()) {
            currentVendor.setLongitude(updatedVendor.getLongitude());
        }
        
        if (updatedVendor.getLatitude() != null && updatedVendor.getLatitude() != currentVendor.getLatitude()) {
            currentVendor.setLatitude(updatedVendor.getLatitude());
        }

//        if (addressChanged || zipChanged) {
//            double[] coordinates = geoLocationService.getCoordinates(currentVendor.getAddress(), currentVendor.getZipCode());
//            if (coordinates != null) {
//                currentVendor.setLatitude(coordinates[0]);
//                currentVendor.setLongitude(coordinates[1]);
//            }
//        }

        vendorRepository.save(currentVendor);
        
        // Clear cache for this vendor to avoid serialization issues
        clearVendorCache(currentVendor.getId(), currentVendor.getEmail());
        
        eventPublisher.publishUpdateVendorProfileEvent(new UpdateVendorProfileEvent(currentVendor.getUsername(), currentVendor.getEmail(), currentVendor.getPassword(), currentVendor.getRole(), currentVendor.getStatus()));
        eventPublisher.publishOfferRelatedVendorDetailsEvent(buildOfferVendorDetailsEvent(currentVendor));
        
        // Only logout and invalidate token if sensitive data was changed
        if (sensitiveDataChanged) {
            logger.info("Sensitive data changed for vendorId: {}. Logging out and invalidating token.", currentVendor.getId());
            try {
                logoutAndInvalidateToken(extractTokenFromContext());
            } catch (Exception e) {
                logger.warn("Failed to extract token for invalidation, but continuing with profile update: {}", e.getMessage());
            }
        } else {
            logger.debug("No sensitive data changed for vendorId: {}. User remains logged in.", currentVendor.getId());
        }
    }
    
    /**
     * Updates a vendor by ID (VENDOR_ADMIN, ADMIN, or SUPER_ADMIN)
     * @param id Vendor ID to update
     * @param updatedVendor Updated vendor data
     */
    public void updateVendorById(Long id, Vendor updatedVendor) {
        Vendor vendor = findVendorById(id);
        Vendor currentUser = getVendorFromContext();
        
        // Check permissions based on current user's role
        if (vendor.getRole() == Role.ADMIN) {
            // Only SUPER_ADMIN can update ADMIN accounts
            if (currentUser.getRole() != Role.SUPER_ADMIN) {
                throw new RuntimeException("Cannot update ADMIN accounts. Only SUPER_ADMIN can manage ADMIN roles.");
            }
        } else if (vendor.getRole() == Role.VENDOR_ADMIN) {
            // ADMIN and SUPER_ADMIN can update VENDOR_ADMIN accounts
            if (currentUser.getRole() != Role.ADMIN && currentUser.getRole() != Role.SUPER_ADMIN) {
                throw new RuntimeException("Cannot update VENDOR_ADMIN accounts. Only ADMIN or SUPER_ADMIN can manage VENDOR_ADMIN roles.");
            }
        } else if (vendor.getRole() == Role.SUPER_ADMIN) {
            // No one can update SUPER_ADMIN accounts
            throw new RuntimeException("Cannot update SUPER_ADMIN accounts.");
        }
        
        // Update fields
        if (updatedVendor.getUsername() != null) {
            vendor.setUsername(updatedVendor.getUsername());
        }
        if (updatedVendor.getEmail() != null) {
            vendor.setEmail(updatedVendor.getEmail());
        }
        if (updatedVendor.getAddress() != null) {
            vendor.setAddress(updatedVendor.getAddress());
        }
        if (updatedVendor.getPhone() != null) {
            vendor.setPhone(updatedVendor.getPhone());
        }
        if (updatedVendor.getPassword() != null) {
            vendor.setPassword(passwordEncoder.encode(updatedVendor.getPassword()));
        }
        // Do not allow role changes via this method (use promote/demote endpoints)
        if (updatedVendor.getStatus() != null) {
            vendor.setStatus(updatedVendor.getStatus());
        }
        if (updatedVendor.getBusinessType() != null) {
            vendor.setBusinessType(updatedVendor.getBusinessType());
        }
        if (updatedVendor.getOperatingHours() != null) {
            vendor.setOperatingHours(updatedVendor.getOperatingHours());
        }
        if (updatedVendor.getSurplusFoodDetails() != null) {
            vendor.setSurplusFoodDetails(updatedVendor.getSurplusFoodDetails());
        }
        if (updatedVendor.getPricingInfo() != null) {
            vendor.setPricingInfo(updatedVendor.getPricingInfo());
        }
        if (updatedVendor.getEnvironmentalCertifications() != null) {
            vendor.setEnvironmentalCertifications(updatedVendor.getEnvironmentalCertifications());
        }
        if (updatedVendor.getImageUrl() != null) {
            vendor.setImageUrl(updatedVendor.getImageUrl());
        }
        if (updatedVendor.getWebsite() != null) {
            vendor.setWebsite(updatedVendor.getWebsite());
        }
        if (updatedVendor.getAboutBusiness() != null) {
            vendor.setAboutBusiness(updatedVendor.getAboutBusiness());
        }
        if (updatedVendor.getContactPerson() != null) {
            vendor.setContactPerson(updatedVendor.getContactPerson());
        }
        if (updatedVendor.getZipCode() != null) {
            vendor.setZipCode(updatedVendor.getZipCode());
        }
        if (updatedVendor.getLatitude() != null) {
            vendor.setLatitude(updatedVendor.getLatitude());
        }
        if (updatedVendor.getLongitude() != null) {
            vendor.setLongitude(updatedVendor.getLongitude());
        }
        
        vendorRepository.save(vendor);
        clearVendorCache(vendor.getId(), vendor.getEmail());
        logger.info("Vendor {} updated by {}", vendor.getEmail(), currentUser.getRole());
    }

    public ResponseEntity<String> deleteVendorProfile(DeleteVendorAccountRequest body) {
        String jwt = extractTokenFromContext();
        Vendor vendor = getVendorFromContext();

        if (body != null) {
            String reason = body.getReason() != null ? body.getReason().trim() : null;
            String message = body.getMessage() != null && !body.getMessage().isBlank() ? body.getMessage().trim() : null;
            if (reason != null || message != null) {
                VendorDeletionFeedback feedback = new VendorDeletionFeedback(vendor.getId(), reason, message);
                vendorDeletionFeedbackRepository.save(feedback);
                logger.info("Saved deletion feedback for vendor {}: reason={}", vendor.getId(), reason);
            }
        }

        vendor.setStatus(Status.DELETED);
        vendorRepository.save(vendor);
        
        eventPublisher.publishUpdateVendorProfileEvent(new UpdateVendorProfileEvent(vendor.getUsername(), vendor.getEmail(), vendor.getPassword(), vendor.getRole(), vendor.getStatus()));
        eventPublisher.publishTokenInvalidationRequest(new TokenRequestEvent(jwt, null));
        eventPublisher.invalidateAllOffers(new AllOffersInvalidationEvent(vendor.getId()));
        return ResponseEntity.ok("Vendor deleted successfully");
    }

    /**
     * Blocks a location from listing offers it cannot be paid for. Customers pay at checkout, so a
     * location without a payout destination would take real money with nowhere to settle it.
     */
    private void requireSellableLocation(Vendor locationVendor) {
        if (locationVendor.getStatus() != Status.ACTIVE) {
            throw new RuntimeException("This location is not active and cannot publish offers.");
        }
        if (!hasPayoutDestination(locationVendor)) {
            throw new RuntimeException("This location has no payout account yet, so it cannot publish offers. " +
                                      "Complete the payment account setup first.");
        }
    }

    public void createOffer(OfferDto request) {
        Vendor currentVendor = getVendorFromContext();
        Vendor locationVendor = currentVendor;  // Default to current vendor
        
        // If current vendor is a worker (VENDOR role with assignedLocationId), 
        // use the location's vendor for offer creation
        if (currentVendor.getRole() == Role.VENDOR && currentVendor.getAssignedLocationId() != null) {
            locationVendor = findVendorById(currentVendor.getAssignedLocationId());
            logger.info("Worker {} creating offer for location: {}", currentVendor.getEmail(), locationVendor.getLocationName());
        }
        
        requireSellableLocation(locationVendor);
        
        // Offer image is now strictly the offer's own image. It may be null
        // if the vendor chose not to attach one; the vendor's profile image is
        // propagated separately via vendorImageUrl below.
        String imageUrl = request.getImageUrl();

        // Validate pickup time is in the future before publishing to Kafka
        validatePickupTimeInFuture(
            request.getPickupDate(), 
            request.getPickupEndTime(), 
            locationVendor.getLatitude(), 
            locationVendor.getLongitude(), 
            "create"
        );

        // Create offer for the location vendor (not the worker)
        OfferCreationEvent event = new OfferCreationEvent(
            locationVendor.getId(),  // Use location's vendor ID
            locationVendor.getLocationName(),
            locationVendor.getChainName(),
            locationVendor.getWebsite(),
            locationVendor.getImageUrl(), // vendor profile image
            request.getName(), 
            request.getDescription(), 
            request.getPrice(), 
            request.getOriginalPrice(), 
            request.getQuantityAvailable(), 
            locationVendor.getAddress(), 
            locationVendor.getZipCode(), 
            locationVendor.getLatitude(), 
            locationVendor.getLongitude(), 
            locationVendor.getBusinessType(), 
            request.getCategory(), 
            request.getDietaryInfo(), 
            request.getAllergenInfo(), 
            request.getPickupDate(), 
            request.getPickupStartTime(), 
            request.getPickupEndTime(), 
            imageUrl, 
            locationVendor.getAverageRating(), 
            locationVendor.getReviewsCount(), 
            request.getExpirationDate(), 
            locationVendor.getCountry()
        );
        eventPublisher.publishOfferCreationEvent(event);
    }

    public List<OfferDto> getActiveOffersForVendor() {
        Vendor currentVendor = getVendorFromContext();
        Long vendorId = currentVendor.getId();
        
        // If worker, get offers for their assigned location
        if (currentVendor.getRole() == Role.VENDOR && currentVendor.getAssignedLocationId() != null) {
            vendorId = currentVendor.getAssignedLocationId();
        }
        
        return offerClient.getActiveOffersForVendor(vendorId);
    }
    
    public List<OfferDto> getAllOffersForVendor() {
        Vendor currentVendor = getVendorFromContext();
        Long vendorId = currentVendor.getId();
        
        // If worker, get offers for their assigned location
        if (currentVendor.getRole() == Role.VENDOR && currentVendor.getAssignedLocationId() != null) {
            vendorId = currentVendor.getAssignedLocationId();
        }
        
        return offerClient.getAllOffersForVendor(vendorId);
    }

    public com.stillfresh.app.sharedentities.dto.VendorStatsResponse getVendorStats(java.time.OffsetDateTime from, java.time.OffsetDateTime to) {
        Vendor currentVendor = getVendorFromContext();
        Long vendorId = currentVendor.getId();
        
        // If worker, get stats for their assigned location
        if (currentVendor.getRole() == Role.VENDOR && currentVendor.getAssignedLocationId() != null) {
            vendorId = currentVendor.getAssignedLocationId();
            logger.info("Worker {} requesting stats for location: {}", currentVendor.getEmail(), vendorId);
        }
        
        String fromStr = (from != null) ? from.toString() : null;
        String toStr = (to != null) ? to.toString() : null;

        String correlationId = java.util.UUID.randomUUID().toString();
        java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);

        vendorStatsResponseListener.registerLatch(correlationId, latch);
        eventPublisher.publishVendorStatsRequest(new com.stillfresh.app.sharedentities.order.events.VendorStatsRequestEvent(
                vendorId, fromStr, toStr, correlationId  // Use location's vendor ID
        ));

        try {
            if (!latch.await(7, java.util.concurrent.TimeUnit.SECONDS)) {
                throw new RuntimeException("Timed out waiting for stats");
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while waiting for stats");
        }

        com.stillfresh.app.sharedentities.order.events.VendorStatsResponseEvent response =
                vendorStatsResponseListener.getResponse(correlationId);

        if (response == null) {
            throw new RuntimeException("No stats response received");
        }
        if (!response.isSuccess()) {
            throw new RuntimeException(response.getErrorMessage() != null ? response.getErrorMessage() : "Stats request failed");
        }

        return response.getStats();
    }

    public void updateOffer(Long offerId, OfferDto request) {
        Vendor currentVendor = getVendorFromContext();
        Vendor locationVendor = currentVendor;  // Default to current vendor
        
        // If current vendor is a worker, use the location's vendor for offer update
        if (currentVendor.getRole() == Role.VENDOR && currentVendor.getAssignedLocationId() != null) {
            locationVendor = findVendorById(currentVendor.getAssignedLocationId());
            logger.info("Worker {} updating offer for location: {}", currentVendor.getEmail(), locationVendor.getLocationName());
        }
        
        requireSellableLocation(locationVendor);
        
        // Validate pickup time is in the future before publishing to Kafka
        // Use pickupDate from request, or if null, we'll validate with existing offer's date in offer-service
        if (request.getPickupDate() != null && request.getPickupEndTime() != null) {
            validatePickupTimeInFuture(
                request.getPickupDate(), 
                request.getPickupEndTime(), 
                locationVendor.getLatitude(), 
                locationVendor.getLongitude(), 
                "update"
            );
        }
        
        // Pass through currency if provided, otherwise leave null (will be determined by offer-service)
        String currency = request.getCurrency();
        OfferDto offerDto = new OfferDto(offerId,
                locationVendor.getLocationName(),
                locationVendor.getChainName(),
                locationVendor.getWebsite(),
                locationVendor.getImageUrl(), // vendor profile image
                request.getName(), request.getDescription(),
                request.getPrice(), request.getOriginalPrice(),
                request.getQuantityAvailable(), request.getDietaryInfo(), request.getAllergenInfo(), request.getImageUrl(),
                request.getRating(), locationVendor.getReviewsCount(), request.getExpirationDate(), true, request.getCreatedAt(),
                locationVendor.getAddress(), locationVendor.getZipCode(), locationVendor.getLatitude(), locationVendor.getLongitude(),
                currency, locationVendor.getBusinessType(), request.getCategory(), request.getPickupDate(), request.getPickupStartTime(), request.getPickupEndTime());
        offerDto.setVendorId(locationVendor.getId());  // Use location's vendor ID
        
        OfferUpdateEvent event = new OfferUpdateEvent(locationVendor.getId(), offerDto);
        eventPublisher.publishUpdateOfferEvent(event);
    }

    public void invalidateOffer(Long offerId) {
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
	    
	    logger.info("Password changed in vendor-service database for vendor ID: {}, email: {}, role: {}", 
	               vendor.getId(), vendor.getEmail(), vendor.getRole());
	    
	    // Publish password update event to Kafka for authorization-service and other services
	    PasswordUpdateEvent passwordUpdateEvent = new PasswordUpdateEvent(
	        vendor.getId(),
	        vendor.getEmail(),
	        encodedPassword,
	        vendor.getRole()
	    );
	    eventPublisher.publishPasswordUpdateEvent(passwordUpdateEvent);
	    
	    // Clear cache after password change
	    clearVendorCache(vendor.getId(), vendor.getEmail());

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
        admin.setStatus(Status.ACTIVE); // Automatically activate the Super-Admin
        
        // Fetch and assign geo-coordinates with fallback
        try {
            double[] coordinates = geoLocationService.getCoordinates(admin.getAddress(), admin.getZipCode());
            if (coordinates != null) {
                admin.setLatitude(coordinates[0]);
                admin.setLongitude(coordinates[1]);
                logger.info("Successfully set coordinates for super admin: {}", admin.getEmail());
            } else {
                logger.warn("Could not geocode address for super admin: {}. Setting default coordinates.", admin.getEmail());
                admin.setLatitude(0.0);
                admin.setLongitude(0.0);
            }
        } catch (Exception e) {
            logger.error("Geocoding failed for super admin: {}. Proceeding with registration without coordinates.", admin.getEmail(), e);
            admin.setLatitude(0.0);
            admin.setLongitude(0.0);
        }
        
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
        clearVendorCache(vendor.getId(), vendor.getEmail());
        syncStatusToAuthorizationService(vendor);
        return vendor.isActive();
    }

    public void deleteVendorById(Long id) {
        Vendor vendor = findVendorById(id);
        Vendor currentUser = getVendorFromContext();

        // Check permissions based on current user's role and target vendor's role
        if (vendor.getRole() == Role.SUPER_ADMIN) {
            throw new RuntimeException("Cannot delete SUPER_ADMIN accounts.");
        } else if (vendor.getRole() == Role.ADMIN) {
            // Only SUPER_ADMIN can delete ADMIN accounts
            if (currentUser.getRole() != Role.SUPER_ADMIN) {
                throw new RuntimeException("Cannot delete ADMIN accounts. Only SUPER_ADMIN can delete ADMIN roles.");
            }
        } else if (vendor.getRole() == Role.VENDOR_ADMIN) {
            // ADMIN and SUPER_ADMIN can delete VENDOR_ADMIN accounts
            if (currentUser.getRole() != Role.ADMIN && currentUser.getRole() != Role.SUPER_ADMIN) {
                throw new RuntimeException("Cannot delete VENDOR_ADMIN accounts. Only ADMIN or SUPER_ADMIN can delete VENDOR_ADMIN roles.");
            }
        }
        // VENDOR role can be deleted by VENDOR_ADMIN, ADMIN, or SUPER_ADMIN (no restriction)

        // Perform the deletion
        vendorRepository.deleteById(id);
        logger.info("Vendor {} deleted by {}", vendor.getEmail(), currentUser.getRole());
    }


    // Method to activate a vendor
    public boolean activateVendor(Long id) {
        Vendor vendor = findVendorById(id);
        vendor.setStatus(Status.ACTIVE); // Set active to true
        vendorRepository.save(vendor);
        clearVendorCache(vendor.getId(), vendor.getEmail());
        syncStatusToAuthorizationService(vendor);
        return vendor.isActive();
    }

    // Method to deactivate a vendor
    public boolean deactivateVendor(Long id) {
        Vendor vendor = findVendorById(id);
        vendor.setStatus(Status.INACTIVE); // Set active to false
        vendorRepository.save(vendor);
        clearVendorCache(vendor.getId(), vendor.getEmail());
        syncStatusToAuthorizationService(vendor);
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
    
    /**
     * Promotes a vendor to VENDOR_ADMIN role (ADMIN and SUPER_ADMIN only)
     * @param vendorId ID of the vendor to promote
     */
    public void promoteVendorToVendorAdmin(Long vendorId) {
        Vendor vendor = findVendorById(vendorId);
        
        // Only allow promoting VENDOR role to VENDOR_ADMIN
        if (vendor.getRole() != Role.VENDOR) {
            throw new RuntimeException("Can only promote VENDOR role to VENDOR_ADMIN. Current role: " + vendor.getRole());
        }
        
        vendor.setRole(Role.VENDOR_ADMIN);
        vendorRepository.save(vendor);
        clearVendorCache(vendor.getId(), vendor.getEmail());
        logger.info("Vendor {} promoted to VENDOR_ADMIN", vendor.getEmail());
    }
    
    /**
     * Demotes a VENDOR_ADMIN to VENDOR role (ADMIN and SUPER_ADMIN only)
     * @param vendorId ID of the VENDOR_ADMIN to demote
     */
    public void demoteVendorAdminToVendor(Long vendorId) {
        Vendor vendor = findVendorById(vendorId);
        
        if (vendor.getRole() != Role.VENDOR_ADMIN) {
            throw new IllegalArgumentException("This vendor is not a VENDOR_ADMIN. Current role: " + vendor.getRole());
        }
        
        // Set the role back to VENDOR
        vendor.setRole(Role.VENDOR);
        vendorRepository.save(vendor);
        clearVendorCache(vendor.getId(), vendor.getEmail());
        logger.info("VENDOR_ADMIN {} demoted to VENDOR", vendor.getEmail());
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
    
    public Vendor registerVendor(Vendor vendor, boolean isAdmin) throws IOException {    	
        if (vendorRepository.existsByEmail(vendor.getEmail()) || vendorRepository.existsByUsername(vendor.getUsername())) {
            throw new IllegalStateException("Vendor already exists with this email or username");
        }  
        vendor.setPassword(passwordEncoder.encode(vendor.getPassword()));
        vendor.setRole(isAdmin ? Role.ADMIN : Role.VENDOR);  // Set role based on input
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
                vendor.setLatitude(0.0);
                vendor.setLongitude(0.0);
            }
        } catch (Exception e) {
            logger.error("Geocoding failed for vendor: {}. Proceeding with registration without coordinates.", vendor.getEmail(), e);
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
    
    public void clearVendorCache(Long vendorId, String email) {
        logger.info("Clearing cache for vendor ID: {} and email: {}", vendorId, email);
        try {
            // Clear all vendor cache entries
            var cache = cacheManager.getCache("vendor");
            if (cache != null) {
                cache.clear();
                logger.info("Successfully cleared vendor cache");
            } else {
                logger.warn("Vendor cache not found");
            }
        } catch (Exception e) {
            logger.error("Failed to clear vendor cache: {}", e.getMessage());
        }
    }

    /**
     * Writes the BCrypt password hash and status into authorization-service over the JSON body.
     * Failures throw so callers that create accounts can roll back instead of leaving a login
     * that still has {@code TEMP_PASSWORD}.
     */
    private void requireAuthorizationCredentialsSynced(Long globalUserId, String encodedPassword, Status status) {
        Map<String, Object> response = authorizationServiceClient.updateUserCredentials(
            new com.stillfresh.app.sharedentities.dto.UpdateUserCredentialsRequest(
                globalUserId, encodedPassword, status));
        if (!Boolean.TRUE.equals(response.get("success"))) {
            throw new RuntimeException("Failed to update credentials in authorization service: " +
                                      response.get("message"));
        }
    }

    /**
     * Mirrors a local status change to authorization-service, which owns login. Without this a
     * deactivated account keeps authenticating, because the credentials live in the auth database.
     * Failures are logged rather than thrown: callers have already persisted the local change, and
     * the alternative (rolling back a deactivation) would leave the account enabled everywhere.
     */
    private void syncStatusToAuthorizationService(Vendor vendor) {
        try {
            Map<String, Object> response = authorizationServiceClient.updateUserStatus(vendor.getId(), vendor.getStatus());
            if (!Boolean.TRUE.equals(response.get("success"))) {
                logger.error("Failed to sync status {} for vendor {} to authorization-service: {}",
                            vendor.getStatus(), vendor.getId(), response.get("message"));
            } else {
                logger.info("Synced status {} for vendor {} to authorization-service", vendor.getStatus(), vendor.getId());
            }
        } catch (Exception e) {
            logger.error("Failed to sync status {} for vendor {} to authorization-service: {}. The account may still be able to log in.",
                        vendor.getStatus(), vendor.getId(), e.getMessage());
        }
    }

    /**
     * Gets the Stripe onboarding link for the authenticated vendor
     * If vendor doesn't have a Stripe account ID, creates one automatically
     * @return Onboarding URL
     */
    public String getStripeOnboardingLink() {
        Vendor vendor = getVendorFromContext();
        
        // 🔹 If vendor doesn't have a Stripe account ID, create one automatically
        if (vendor.getStripeAccountId() == null || vendor.getStripeAccountId().isEmpty()) {
            logger.info("Vendor {} does not have a Stripe account ID. Creating one now...", vendor.getEmail());
            
            if (paymentClient == null) {
                logger.error("Payment client is not available");
                throw new RuntimeException("Payment service is unavailable. Please try again later.");
            }
            
            try {
                // Create Stripe Connect account for the vendor
                com.stillfresh.app.vendorservice.client.PaymentClient.StripeConnectResponse response = paymentClient.createConnectAccount(
                    vendor.getEmail(), 
                    vendor.getUsername()
                );
                
                if (response != null && response.getValue() != null && !response.getValue().isEmpty()) {
                    String stripeAccountId = response.getValue().trim();
                    vendor.setStripeAccountId(stripeAccountId);
                    vendorRepository.save(vendor);
                    logger.info("Successfully created Stripe Connect account: {} for vendor: {}", stripeAccountId, vendor.getEmail());
                    
                    // Clear cache to reflect the new Stripe account ID
                    clearVendorCache(vendor.getId(), vendor.getEmail());
                } else {
                    logger.error("Failed to create Stripe Connect account for vendor: {}. Empty response.", vendor.getEmail());
                    throw new RuntimeException("Failed to create Stripe account. Please try again later.");
                }
            } catch (Exception e) {
                logger.error("Error creating Stripe Connect account for vendor {}: {}", vendor.getEmail(), e.getMessage(), e);
                
                // Provide more user-friendly error message for unsupported countries
                String errorMessage = e.getMessage();
                if (errorMessage != null && errorMessage.contains("country_unsupported")) {
                    throw new RuntimeException("Stripe Connect is not available in your country yet. " +
                             "Please contact support for alternative payment setup options.");
                }
                
                throw new RuntimeException("Failed to create Stripe account: " + e.getMessage());
            }
        }
        
        if (paymentClient == null) {
            logger.error("Payment client is not available");
            throw new RuntimeException("Payment service is unavailable. Please try again later.");
        }
        
        try {
            logger.info("Getting Stripe onboarding link for vendor: {} (account: {})", 
                        vendor.getEmail(), vendor.getStripeAccountId());
            com.stillfresh.app.vendorservice.client.PaymentClient.StripeConnectResponse response = paymentClient.createAccountLink(vendor.getStripeAccountId());
            
            if (response != null && response.getValue() != null && !response.getValue().isEmpty()) {
                String onboardingUrl = response.getValue().trim();
                logger.info("Successfully retrieved onboarding link for vendor: {}", vendor.getEmail());
                return onboardingUrl;
            } else {
                logger.error("Failed to get onboarding link. Empty response.");
                throw new RuntimeException("Failed to generate onboarding link. Please try again later.");
            }
        } catch (Exception e) {
            logger.error("Error getting Stripe onboarding link for vendor {}: {}", vendor.getEmail(), e.getMessage(), e);
            throw new RuntimeException("Failed to get onboarding link: " + e.getMessage());
        }
    }

    /**
     * Checks if the authenticated vendor's payment account is ready to receive payments
     * Works with both Stripe Connect and MoR
     * @return true if account is ready, false otherwise
     */
    public boolean isStripeAccountReady() {
        // For backward compatibility, delegate to new unified method
        Vendor vendor = getVendorFromContext();
        return paymentRoutingService.isAccountReady(vendor);
    }
    
    /**
     * Checks if the authenticated vendor has a payment account (Stripe Connect or MoR)
     * @return true if vendor has a payment account, false otherwise
     */
    public boolean hasStripeAccount() {
        Vendor vendor = getVendorFromContext();
        PaymentProvider provider = paymentRoutingService.determineProvider(vendor);
        
        switch (provider) {
            case STRIPE:
                return vendor.getStripeAccountId() != null && !vendor.getStripeAccountId().isEmpty();
            case MOR:
                // For MoR, check if bank details are provided
                return vendor.getBankAccountNumber() != null && !vendor.getBankAccountNumber().isEmpty();
            default:
            return false;
        }
    }

    /**
     * Gets detailed Stripe account information for the authenticated vendor
     * @return Account details as a map
     */
    public Map<String, Object> getStripeAccountDetails() {
        Vendor vendor = getVendorFromContext();
        
        if (vendor.getStripeAccountId() == null || vendor.getStripeAccountId().isEmpty()) {
            throw new RuntimeException("Vendor does not have a Stripe account. Please complete onboarding first.");
        }
        
        if (paymentClient == null) {
            throw new RuntimeException("Payment service is unavailable. Please try again later.");
        }
        
        try {
            return paymentClient.getAccountDetails(vendor.getStripeAccountId());
        } catch (Exception e) {
            logger.error("Error retrieving Stripe account details for vendor {}: {}", vendor.getEmail(), e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve account details: " + e.getMessage());
        }
    }

    /**
     * Creates a login link for the Stripe Express Dashboard
     * @return Login URL for the Stripe dashboard
     */
    public String getStripeLoginLink() {
        Vendor vendor = getVendorFromContext();
        
        if (vendor.getStripeAccountId() == null || vendor.getStripeAccountId().isEmpty()) {
            throw new RuntimeException("Vendor does not have a Stripe account. Please complete onboarding first.");
        }
        
        if (paymentClient == null) {
            throw new RuntimeException("Payment service is unavailable. Please try again later.");
        }
        
        try {
            com.stillfresh.app.vendorservice.client.PaymentClient.StripeConnectResponse response = paymentClient.createLoginLink(vendor.getStripeAccountId());
            if (response != null && response.getValue() != null && !response.getValue().isEmpty()) {
                return response.getValue();
            } else {
                throw new RuntimeException("Failed to create login link. Empty response.");
            }
        } catch (Exception e) {
            logger.error("Error creating Stripe login link for vendor {}: {}", vendor.getEmail(), e.getMessage(), e);
            throw new RuntimeException("Failed to create login link: " + e.getMessage());
        }
    }

    /**
     * Gets payout history for the authenticated vendor
     * @param limit Maximum number of payouts to retrieve (default: 10, max: 100)
     * @return List of payouts as maps
     */
    public List<Map<String, Object>> getStripePayouts(Integer limit) {
        Vendor vendor = getVendorFromContext();
        
        if (vendor.getStripeAccountId() == null || vendor.getStripeAccountId().isEmpty()) {
            throw new RuntimeException("Vendor does not have a Stripe account. Please complete onboarding first.");
        }
        
        if (paymentClient == null) {
            throw new RuntimeException("Payment service is unavailable. Please try again later.");
        }
        
        try {
            return paymentClient.getPayouts(vendor.getStripeAccountId(), limit);
        } catch (Exception e) {
            logger.error("Error retrieving Stripe payouts for vendor {}: {}", vendor.getEmail(), e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve payouts: " + e.getMessage());
        }
    }

    /**
     * Gets a specific payout by ID for the authenticated vendor
     * @param payoutId Payout ID
     * @return Payout details as a map
     */
    public Map<String, Object> getStripePayout(String payoutId) {
        Vendor vendor = getVendorFromContext();
        
        if (vendor.getStripeAccountId() == null || vendor.getStripeAccountId().isEmpty()) {
            throw new RuntimeException("Vendor does not have a Stripe account. Please complete onboarding first.");
        }
        
        if (paymentClient == null) {
            throw new RuntimeException("Payment service is unavailable. Please try again later.");
        }
        
        try {
            return paymentClient.getPayout(vendor.getStripeAccountId(), payoutId);
        } catch (Exception e) {
            logger.error("Error retrieving Stripe payout {} for vendor {}: {}", payoutId, vendor.getEmail(), e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve payout: " + e.getMessage());
        }
    }

    /**
     * Gets balance information for the authenticated vendor
     * @return Balance information as a map
     */
    public Map<String, Object> getStripeBalance() {
        Vendor vendor = getVendorFromContext();
        
        if (vendor.getStripeAccountId() == null || vendor.getStripeAccountId().isEmpty()) {
            throw new RuntimeException("Vendor does not have a Stripe account. Please complete onboarding first.");
        }
        
        if (paymentClient == null) {
            throw new RuntimeException("Payment service is unavailable. Please try again later.");
        }
        
        try {
            return paymentClient.getBalance(vendor.getStripeAccountId());
        } catch (Exception e) {
            logger.error("Error retrieving Stripe balance for vendor {}: {}", vendor.getEmail(), e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve balance: " + e.getMessage());
        }
    }

    /**
     * Gets transaction history for the authenticated vendor
     * @param limit Maximum number of transactions to retrieve (default: 10, max: 100)
     * @return List of transactions as maps
     */
    public List<Map<String, Object>> getStripeTransactions(Integer limit) {
        Vendor vendor = getVendorFromContext();
        
        if (vendor.getStripeAccountId() == null || vendor.getStripeAccountId().isEmpty()) {
            throw new RuntimeException("Vendor does not have a Stripe account. Please complete onboarding first.");
        }
        
        if (paymentClient == null) {
            throw new RuntimeException("Payment service is unavailable. Please try again later.");
        }
        
        try {
            return paymentClient.getTransactions(vendor.getStripeAccountId(), limit);
        } catch (Exception e) {
            logger.error("Error retrieving Stripe transactions for vendor {}: {}", vendor.getEmail(), e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve transactions: " + e.getMessage());
        }
    }

    /**
     * Gets verification requirements for the authenticated vendor
     * @return Requirements details as a map
     */
    public Map<String, Object> getStripeRequirements() {
        Vendor vendor = getVendorFromContext();
        
        if (vendor.getStripeAccountId() == null || vendor.getStripeAccountId().isEmpty()) {
            throw new RuntimeException("Vendor does not have a Stripe account. Please complete onboarding first.");
        }
        
        if (paymentClient == null) {
            throw new RuntimeException("Payment service is unavailable. Please try again later.");
        }
        
        try {
            return paymentClient.getRequirements(vendor.getStripeAccountId());
        } catch (Exception e) {
            logger.error("Error retrieving Stripe requirements for vendor {}: {}", vendor.getEmail(), e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve requirements: " + e.getMessage());
        }
    }

    /**
     * Initializes payment account for vendor based on their country
     * Creates Stripe Connect account if country is supported, otherwise sets up MoR model
     * @param vendor Vendor entity
     */
    private void initializeVendorPaymentAccount(Vendor vendor) {
        // Get country from vendor (should already be converted to ISO code, but ensure it is)
        String countryInput = vendor.getCountry();
        
        // If no country, skip payment account creation
        if (countryInput == null || countryInput.isEmpty()) {
            logger.warn("Country not set for vendor: {}. Skipping payment account creation.", vendor.getEmail());
            return;
        }
        
        // Ensure country is in ISO 2-letter format (convert if needed)
        String countryCode = countryCodeConverter.convertToIsoCode(countryInput);
        if (countryCode == null) {
            // If conversion fails, assume it's already a code or use as-is
            countryCode = countryInput.trim().toUpperCase();
            logger.debug("Country '{}' could not be converted, using as-is (assuming it's already a code)", countryInput);
        } else if (!countryInput.equals(countryCode)) {
            logger.info("Converted country '{}' to ISO code '{}' during payment account initialization", countryInput, countryCode);
        }
        
        // Determine payment provider and payout model based on country code
        PaymentProvider provider = paymentProviderService.determineProvider(countryCode);
        PayoutModel payoutModel = paymentProviderService.determinePayoutModel(countryCode);
        boolean stripeSupported = paymentProviderService.isStripeSupported(countryCode);
        
        // Update vendor with country code and provider info
        vendor.setCountry(countryCode);
        vendor.setPaymentProvider(provider);
        vendor.setPayoutModel(payoutModel);
        vendor.setStripeSupported(stripeSupported);
        
        // Save vendor with country and payment info immediately
        vendorRepository.save(vendor);
        logger.info("Saved vendor with country and payment info: {} (country: {}, provider: {}, model: {})", 
                   vendor.getEmail(), countryCode, provider, payoutModel);
        
        try {
            switch (provider) {
                case STRIPE:
                    initializeStripeAccount(vendor);
                    break;
                case MOR:
                    initializeMoRAccount(vendor);
                    break;
                default:
                    logger.warn("Unknown payment provider for vendor: {}", vendor.getEmail());
            }
        } catch (Exception e) {
            logger.error("Error initializing payment account for vendor: {}. Error: {}", 
                        vendor.getEmail(), e.getMessage(), e);
            // Don't fail verification if payment account creation fails
        }
    }
    
    /**
     * Initializes Stripe Connect account for vendor
     */
    private void initializeStripeAccount(Vendor vendor) {
        if (vendor.getStripeAccountId() != null && !vendor.getStripeAccountId().isEmpty()) {
            logger.info("Vendor {} already has Stripe Connect account: {}", vendor.getEmail(), vendor.getStripeAccountId());
            return;
        }
        
        if (paymentClient == null) {
            logger.warn("Payment client is not available. Cannot create Stripe account.");
            return;
        }
        
        try {
            logger.info("Creating Stripe Connect account for verified vendor: {} ({})", vendor.getUsername(), vendor.getEmail());
            com.stillfresh.app.vendorservice.client.PaymentClient.StripeConnectResponse response = paymentClient.createConnectAccount(
                vendor.getEmail(), 
                vendor.getUsername()
            );
            
            if (response != null && response.getValue() != null && !response.getValue().isEmpty()) {
                String stripeAccountId = response.getValue().trim();
                vendor.setStripeAccountId(stripeAccountId);
                vendorRepository.save(vendor);
                logger.info("Successfully created and saved Stripe Connect account: {} for vendor: {}", 
                           stripeAccountId, vendor.getEmail());
                
                // Clear cache to reflect the new Stripe account ID
                clearVendorCache(vendor.getId(), vendor.getEmail());
            } else {
                logger.warn("Failed to create Stripe Connect account for vendor: {}. Empty response.", vendor.getEmail());
            }
        } catch (Exception e) {
            logger.error("Error creating Stripe Connect account for vendor: {}. Error: {}", 
                        vendor.getEmail(), e.getMessage(), e);
            // Check if it's an unsupported country error
            if (e.getMessage() != null && e.getMessage().contains("country_unsupported")) {
                logger.info("Stripe not supported for vendor country. Will use MoR model instead.");
                vendor.setPaymentProvider(PaymentProvider.MOR);
                vendor.setPayoutModel(PayoutModel.MOR);
                vendor.setStripeSupported(false);
                initializeMoRAccount(vendor);
            }
        }
    }
    
    /**
     * Initializes MoR (Merchant of Record) account for vendor
     */
    private void initializeMoRAccount(Vendor vendor) {
        logger.info("Setting up MoR model for vendor: {} ({})", vendor.getUsername(), vendor.getEmail());
        
        vendor.setPayoutModel(PayoutModel.MOR);
        vendor.setPaymentProvider(PaymentProvider.MOR);
        vendor.setBalance(java.math.BigDecimal.ZERO);
        vendor.setStripeSupported(false);
        
        vendorRepository.save(vendor);
        logger.info("MoR model initialized for vendor: {}. Vendor needs to provide bank details to receive payouts.", 
                   vendor.getEmail());
        
        // Clear cache
        clearVendorCache(vendor.getId(), vendor.getEmail());
    }
    
    /**
     * Gets onboarding link for vendor based on their payment provider
     * @return Onboarding URL
     */
    public String getPaymentOnboardingLink() {
        Vendor vendor = getVendorFromContext();
        
        PaymentProvider provider = paymentRoutingService.determineProvider(vendor);
        com.stillfresh.app.vendorservice.service.payment.VendorPayoutProcessor processor = 
            paymentRoutingService.getProcessor(vendor);
        
        String accountId;
        switch (provider) {
            case STRIPE:
                accountId = vendor.getStripeAccountId();
                // ✅ Auto-create Stripe account if it doesn't exist (same as getStripeOnboardingLink)
                if (accountId == null || accountId.isEmpty()) {
                    logger.info("Vendor {} does not have a Stripe account ID. Creating one now...", vendor.getEmail());
                    
                    if (paymentClient == null) {
                        logger.error("Payment client is not available");
                        throw new RuntimeException("Payment service is unavailable. Please try again later.");
                    }
                    
                    try {
                        // Create Stripe Connect account for the vendor
                        com.stillfresh.app.vendorservice.client.PaymentClient.StripeConnectResponse response = paymentClient.createConnectAccount(
                            vendor.getEmail(), 
                            vendor.getUsername()
                        );
                        
                        if (response != null && response.getValue() != null && !response.getValue().isEmpty()) {
                            accountId = response.getValue().trim();
                            vendor.setStripeAccountId(accountId);
                            vendorRepository.save(vendor);
                            logger.info("Successfully created Stripe Connect account: {} for vendor: {}", accountId, vendor.getEmail());
                            
                            // Clear cache to reflect the new Stripe account ID
                            clearVendorCache(vendor.getId(), vendor.getEmail());
                        } else {
                            logger.error("Failed to create Stripe Connect account for vendor: {}. Empty response.", vendor.getEmail());
                            throw new RuntimeException("Failed to create Stripe account. Please try again later.");
                        }
                    } catch (Exception e) {
                        logger.error("Error creating Stripe Connect account for vendor {}: {}", vendor.getEmail(), e.getMessage(), e);
                        
                        // Provide more user-friendly error message for unsupported countries
                        String errorMessage = e.getMessage();
                        if (errorMessage != null && errorMessage.contains("country_unsupported")) {
                            throw new RuntimeException("Stripe Connect is not available in your country yet. " +
                                     "Please contact support for alternative payment setup options.");
                        }
                        
                        throw new RuntimeException("Failed to create Stripe account: " + e.getMessage());
                    }
                }
                break;
            case MOR:
                // For MoR, use email as identifier
                accountId = vendor.getEmail();
                if (accountId == null || accountId.isEmpty()) {
                    throw new RuntimeException("Vendor email is required.");
                }
                break;
            default:
                throw new RuntimeException("Unknown payment provider for vendor.");
        }
        
        try {
            return processor.createOnboardingLink(accountId);
        } catch (Exception e) {
            logger.error("Error getting onboarding link for vendor {}: {}", vendor.getEmail(), e.getMessage(), e);
            throw new RuntimeException("Failed to get onboarding link: " + e.getMessage());
        }
    }
    
    /**
     * Gets payment account status for vendor
     * Works with both Stripe Connect and MoR
     * @return Map containing account status information
     */
    public Map<String, Object> getPaymentAccountStatus() {
        Vendor vendor = getVendorFromContext();
        
        PaymentProvider provider = paymentRoutingService.determineProvider(vendor);
        PayoutModel payoutModel = vendor.getPayoutModel();
        boolean isReady = paymentRoutingService.isAccountReady(vendor);
        boolean hasAccount = false;
        String accountId = null;
        
        switch (provider) {
            case STRIPE:
                hasAccount = vendor.getStripeAccountId() != null && !vendor.getStripeAccountId().isEmpty();
                accountId = vendor.getStripeAccountId();
                break;
            case MOR:
                hasAccount = hasPayoutDestination(vendor);
                accountId = vendor.getEmail(); // Use email for MoR
                break;
        }
        
        Map<String, Object> status = new HashMap<>();
        status.put("isReady", isReady);
        status.put("hasAccount", hasAccount);
        status.put("provider", provider.toString());
        status.put("payoutModel", payoutModel != null ? payoutModel.toString() : null);
        status.put("accountId", accountId);
        status.put("country", vendor.getCountry());
        status.put("stripeSupported", vendor.getStripeSupported() != null ? vendor.getStripeSupported() : false);
        
        // Add MoR-specific fields
        if (provider == PaymentProvider.MOR) {
            status.put("balance", vendor.getBalance() != null ? vendor.getBalance() : java.math.BigDecimal.ZERO);
            status.put("manualPayoutMethod", vendor.getManualPayoutMethod());
            status.put("hasBankDetails", hasAccount);
        }
        
        status.put("message", isReady ? 
            "Your payment account is ready to receive payments." : 
            "Your payment account is not ready. Please complete onboarding.");
        
        return status;
    }
    
    // ========== MoR (Merchant of Record) Specific Methods ==========
    
    /**
     * Gets balance for MoR vendor
     * @return Map containing balance information
     */
    public Map<String, Object> getMoRBalance() {
        Vendor vendor = getVendorFromContext();
        
        if (vendor.getPayoutModel() != PayoutModel.MOR) {
            throw new RuntimeException("Vendor is not using MoR model");
        }
        
        Map<String, Object> balance = new HashMap<>();
        balance.put("balance", vendor.getBalance() != null ? vendor.getBalance() : java.math.BigDecimal.ZERO);
        balance.put("currency", "EUR"); // Default currency, can be made dynamic
        balance.put("payoutModel", "MOR");
        balance.put("hasBankDetails", vendor.getBankAccountNumber() != null && !vendor.getBankAccountNumber().isEmpty());
        
        return balance;
    }
    
    /**
     * Gets transaction history for MoR vendor
     * @param limit Maximum number of transactions
     * @return List of transactions
     */
    public List<Map<String, Object>> getMoRTransactions(Integer limit) {
        Vendor vendor = getVendorFromContext();
        
        if (vendor.getPayoutModel() != PayoutModel.MOR) {
            throw new RuntimeException("Vendor is not using MoR model");
        }
        
        int transactionLimit = limit != null ? Math.min(limit, 100) : 50;
        
        List<VendorBalanceTransaction> transactions = 
            balanceTransactionRepository.findByVendorIdOrderByCreatedAtDesc(vendor.getId());
        
        return transactions.stream()
            .limit(transactionLimit)
            .map(t -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", t.getId());
                map.put("amount", t.getAmount());
                map.put("currency", t.getCurrency());
                map.put("type", t.getType());
                map.put("description", t.getDescription());
                map.put("orderId", t.getOrderId());
                map.put("payoutId", t.getPayoutId());
                map.put("createdAt", t.getCreatedAt());
                return map;
            })
            .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * Returns the current banking data for the authenticated MoR vendor.
     * Sensitive fields (account number, IBAN) are masked before leaving the backend.
     *
     * @return MorBankDetailsResponse with masked account number and masked IBAN
     */
    public com.stillfresh.app.vendorservice.dto.MorBankDetailsResponse getMoRBankDetails() {
        Vendor vendor = getVendorFromContext();

        if (vendor.getPayoutModel() != PayoutModel.MOR) {
            throw new RuntimeException("Vendor is not using MoR model");
        }

        return toMoRBankDetailsResponse(vendor);
    }

    private com.stillfresh.app.vendorservice.dto.MorBankDetailsResponse toMoRBankDetailsResponse(Vendor vendor) {
        com.stillfresh.app.vendorservice.dto.MorBankDetailsResponse response =
            new com.stillfresh.app.vendorservice.dto.MorBankDetailsResponse();

        response.setHasBankDetails(PaymentRoutingService.hasBankDestination(vendor));
        response.setHolderName(vendor.getBankAccountHolderName());
        response.setBankName(vendor.getBankName());
        response.setSwiftCode(vendor.getBankSwiftCode());
        response.setAccountNumberMasked(maskSensitive(vendor.getBankAccountNumber(), 4));
        response.setIbanMasked(maskSensitive(vendor.getBankIban(), 4));
        response.setManualPayoutMethod(vendor.getManualPayoutMethod());

        logger.info("Returning masked MoR bank details for vendor id={} email={}",
            vendor.getId(), vendor.getEmail());

        return response;
    }

    /**
     * Masks a sensitive value by keeping only the last {@code visibleSuffix} characters visible
     * and replacing the rest with '*'. Returns null for null/blank input.
     *
     * @param value           raw sensitive value (e.g. account number, IBAN)
     * @param visibleSuffix   number of trailing characters to keep visible
     * @return masked value safe to transmit to a client, or null if input was null/blank
     */
    private String maskSensitive(String value, int visibleSuffix) {
        if (value == null) {
            return null;
        }
        String cleaned = value.replaceAll("\\s+", "");
        if (cleaned.isEmpty()) {
            return null;
        }
        if (cleaned.length() <= visibleSuffix) {
            return "*".repeat(cleaned.length());
        }
        int maskedLength = cleaned.length() - visibleSuffix;
        return "*".repeat(maskedLength) + cleaned.substring(maskedLength);
    }

    private static final java.util.regex.Pattern IBAN_PATTERN =
        java.util.regex.Pattern.compile("^[A-Z]{2}[0-9]{2}[A-Z0-9]{11,30}$");
    private static final java.util.regex.Pattern SWIFT_PATTERN =
        java.util.regex.Pattern.compile("^[A-Z]{6}[A-Z0-9]{2}([A-Z0-9]{3})?$");
    private static final java.util.regex.Pattern ACCOUNT_NUMBER_PATTERN =
        java.util.regex.Pattern.compile("^[A-Z0-9\\-]{5,34}$");
    
    /**
     * Strips spaces from a bank field and upper-cases it. Domestic account numbers may keep
     * hyphens (Serbian {@code xxx-xxxxxxxxx-xx} format); IBAN and SWIFT use
     * {@link #normalizeAlphanumericBankValue} instead.
     */
    private String normalizeBankValue(String value) {
        if (value == null) {
            return null;
        }
        String cleaned = value.replaceAll("[\\s]+", "").toUpperCase();
        return cleaned.isEmpty() ? null : cleaned;
    }

    /**
     * IBAN and SWIFT are alphanumeric only. Users (and many mobile keyboards) insert spaces or
     * hyphens for readability — strip those before validating, otherwise a correctly typed IBAN
     * is rejected as a format error.
     */
    private String normalizeAlphanumericBankValue(String value) {
        if (value == null) {
            return null;
        }
        String cleaned = value.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
        return cleaned.isEmpty() ? null : cleaned;
    }
    
    /**
     * Validates an IBAN structurally and with the ISO 13616 mod-97 checksum. Payout files are
     * rejected by the bank days later, so a typo caught here saves a failed settlement cycle.
     */
    private void validateIban(String iban) {
        if (!IBAN_PATTERN.matcher(iban).matches()) {
            throw new RuntimeException("IBAN format is invalid. Expected 2 country letters, 2 check digits and up to 30 alphanumeric characters.");
        }
        String rearranged = iban.substring(4) + iban.substring(0, 4);
        StringBuilder numeric = new StringBuilder();
        for (char c : rearranged.toCharArray()) {
            if (Character.isDigit(c)) {
                numeric.append(c);
            } else {
                numeric.append(Character.getNumericValue(c));
            }
        }
        java.math.BigInteger value = new java.math.BigInteger(numeric.toString());
        if (!value.mod(java.math.BigInteger.valueOf(97)).equals(java.math.BigInteger.ONE)) {
            throw new RuntimeException("IBAN checksum is invalid. Please double-check the IBAN.");
        }
    }
    
    /**
     * Submits bank details for MoR vendor.
     * Only the keys present in the payload are applied, so a partial update cannot silently wipe
     * previously stored details.
     *
     * @param bankDetails Map containing bank account information
     */
    @org.springframework.transaction.annotation.Transactional
    public void submitBankDetails(Map<String, String> bankDetails) {
        Vendor vendor = getVendorFromContext();
        
        if (vendor.getPayoutModel() != PayoutModel.MOR) {
            throw new RuntimeException("Vendor is not using MoR model");
        }
        
        applyBankDetails(vendor, bankDetails);
        logger.info("Bank details submitted for MoR vendor: {}", vendor.getEmail());
    }

    /**
     * Applies a partial MoR bank-details update to {@code vendor}, persists it, and emails the
     * vendor when an existing payout destination is replaced.
     */
    private void applyBankDetails(Vendor vendor, Map<String, String> bankDetails) {
        if (bankDetails == null || bankDetails.isEmpty()) {
            throw new RuntimeException("No bank details provided");
        }
        
        String previousIban = vendor.getBankIban();
        String previousAccountNumber = vendor.getBankAccountNumber();
        
        if (bankDetails.containsKey("holderName")) {
            String holderName = bankDetails.get("holderName");
            if (holderName == null || holderName.trim().isEmpty()) {
                throw new RuntimeException("Account holder name cannot be empty");
            }
            vendor.setBankAccountHolderName(holderName.trim());
        }
        
        if (bankDetails.containsKey("bankName")) {
            String bankName = bankDetails.get("bankName");
            vendor.setBankName(bankName != null && !bankName.trim().isEmpty() ? bankName.trim() : null);
        }
        
        if (bankDetails.containsKey("iban")) {
            String iban = normalizeAlphanumericBankValue(bankDetails.get("iban"));
            if (iban != null) {
                validateIban(iban);
            }
            vendor.setBankIban(iban);
        }
        
        if (bankDetails.containsKey("accountNumber")) {
            String accountNumber = normalizeBankValue(bankDetails.get("accountNumber"));
            if (accountNumber != null && !ACCOUNT_NUMBER_PATTERN.matcher(accountNumber).matches()) {
                throw new RuntimeException("Account number format is invalid.");
            }
            vendor.setBankAccountNumber(accountNumber);
        }
        
        if (bankDetails.containsKey("swiftCode")) {
            String swift = normalizeAlphanumericBankValue(bankDetails.get("swiftCode"));
            if (swift != null && !SWIFT_PATTERN.matcher(swift).matches()) {
                throw new RuntimeException("SWIFT/BIC format is invalid. Expected 8 or 11 characters.");
            }
            vendor.setBankSwiftCode(swift);
        }
        
        if (bankDetails.get("payoutMethod") != null) {
            try {
                vendor.setManualPayoutMethod(ManualPayoutMethod.valueOf(bankDetails.get("payoutMethod").toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Invalid payout method: " + bankDetails.get("payoutMethod"));
            }
        }
        
        // A payout needs somewhere to land: either an IBAN or a domestic account number.
        if (vendor.getBankIban() == null && vendor.getBankAccountNumber() == null) {
            throw new RuntimeException("Either an IBAN or an account number is required for payouts.");
        }
        if (vendor.getBankAccountHolderName() == null || vendor.getBankAccountHolderName().trim().isEmpty()) {
            throw new RuntimeException("Account holder name is required for payouts.");
        }
        
        vendorRepository.save(vendor);
        clearVendorCache(vendor.getId(), vendor.getEmail());
        
        boolean destinationChanged = !java.util.Objects.equals(previousIban, vendor.getBankIban())
            || !java.util.Objects.equals(previousAccountNumber, vendor.getBankAccountNumber());
        boolean hadDestinationBefore = previousIban != null || previousAccountNumber != null;
        if (destinationChanged && hadDestinationBefore) {
            notifyBankDetailsChanged(vendor);
        }
    }
    
    /**
     * Warns the vendor out-of-band that their payout destination changed. If an attacker takes over
     * an account, this email is the vendor's only chance to notice before the next payout leaves.
     */
    private void notifyBankDetailsChanged(Vendor vendor) {
        String masked = vendor.getBankIban() != null
            ? maskSensitive(vendor.getBankIban(), 4)
            : maskSensitive(vendor.getBankAccountNumber(), 4);
        try {
            emailService.sendEmail(
                vendor.getEmail(),
                "Your StillFresh payout account was changed",
                "The payout bank account for your StillFresh account was just changed to " + masked + ".\n\n"
                    + "If you did not make this change, contact support immediately and change your password.");
        } catch (Exception e) {
            logger.error("Failed to send bank-details change notification to {}: {}", vendor.getEmail(), e.getMessage());
        }
    }
    
    /**
     * Gets payout history for MoR vendor
     * @return List of payouts
     */
    public List<Map<String, Object>> getMoRPayouts() {
        Vendor vendor = getVendorFromContext();
        
        if (vendor.getPayoutModel() != PayoutModel.MOR) {
            throw new RuntimeException("Vendor is not using MoR model");
        }
        
        List<VendorPayout> payouts = payoutRepository.findByVendorIdOrderByRequestedAtDesc(vendor.getId());
        
        return payouts.stream()
            .map(p -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", p.getId());
                map.put("amount", p.getAmount());
                map.put("currency", p.getCurrency());
                map.put("method", p.getMethod());
                map.put("status", p.getStatus());
                map.put("requestedAt", p.getRequestedAt());
                map.put("processedAt", p.getProcessedAt());
                map.put("transactionReference", p.getTransactionReference());
                return map;
            })
            .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * Requests a manual payout for MoR vendor
     * @param amount Amount in cents
     * @param currency Currency code
     * @param description Description
     * @return Payout ID
     */
    @org.springframework.transaction.annotation.Transactional
    public String requestMoRPayout(long amount, String currency, String description) {
        Vendor vendor = getVendorFromContext();
        
        if (vendor.getPayoutModel() != PayoutModel.MOR) {
            throw new RuntimeException("Vendor is not using MoR model");
        }
        
        if (vendor.getManualPayoutMethod() == null) {
            throw new RuntimeException("Payout method not configured. Please submit bank details first.");
        }
        
        com.stillfresh.app.vendorservice.service.payment.MoRPayoutProcessor morProcessor = 
            (com.stillfresh.app.vendorservice.service.payment.MoRPayoutProcessor) 
            paymentRoutingService.getProcessor(vendor);
        
        return morProcessor.requestManualPayout(vendor.getId(), amount, currency, description);
    }
    
    /**
     * Updates MoR vendor balance (called by payment-service after successful payment)
     * @param vendorId Vendor ID
     * @param amount Amount in cents to add
     * @param currency Currency code
     * @param orderId Order ID (optional)
     * @param description Description
     */
    @org.springframework.transaction.annotation.Transactional
    public void updateMoRBalance(Long vendorId, long amount, String currency, Long orderId, String description) {
        Optional<Vendor> vendorOpt = vendorRepository.findById(vendorId);
        if (vendorOpt.isEmpty()) {
            throw new RuntimeException("Vendor not found: " + vendorId);
        }
        
        Vendor vendor = vendorOpt.get();
        
        if (vendor.getPayoutModel() != PayoutModel.MOR) {
            throw new RuntimeException("Vendor is not using MoR model. Cannot update balance.");
        }
        
        com.stillfresh.app.vendorservice.service.payment.MoRPayoutProcessor morProcessor = 
            (com.stillfresh.app.vendorservice.service.payment.MoRPayoutProcessor) 
            paymentRoutingService.getProcessor(vendor);
        
        morProcessor.addToBalance(vendorId, amount, currency, description, orderId);
        
        logger.info("Updated MoR balance for vendor {}: +{} {}", vendorId, amount, currency);
    }
    
    // ========== Admin Methods for MoR Payment Management ==========
    
    /**
     * Gets all pending payouts for MoR vendors (admin only)
     * @return List of payouts with vendor information
     */
    public List<Map<String, Object>> getAllMoRPendingPayouts() {
        List<String> pendingStatuses = java.util.Arrays.asList("PENDING", "PROCESSING");
        List<VendorPayout> payouts = payoutRepository.findByStatusInOrderByRequestedAtDesc(pendingStatuses);
        
        return payouts.stream()
            .map(payout -> {
                Optional<Vendor> vendorOpt = vendorRepository.findById(payout.getVendorId());
                Vendor vendor = vendorOpt.orElse(null);
                
                Map<String, Object> map = new HashMap<>();
                map.put("id", payout.getId());
                map.put("vendorId", payout.getVendorId());
                map.put("vendorName", vendor != null ? vendor.getUsername() : "Unknown");
                map.put("vendorEmail", vendor != null ? vendor.getEmail() : "Unknown");
                map.put("amount", payout.getAmount());
                map.put("currency", payout.getCurrency());
                map.put("method", payout.getMethod());
                map.put("status", payout.getStatus());
                map.put("requestedAt", payout.getRequestedAt());
                map.put("processedAt", payout.getProcessedAt());
                map.put("transactionReference", payout.getTransactionReference());
                map.put("notes", payout.getNotes());
                
                // Include bank details for payout processing
                if (vendor != null && vendor.getPayoutModel() == PayoutModel.MOR) {
                    Map<String, Object> bankDetails = new HashMap<>();
                    bankDetails.put("bankAccountHolderName", vendor.getBankAccountHolderName());
                    bankDetails.put("bankAccountNumber", vendor.getBankAccountNumber());
                    bankDetails.put("bankName", vendor.getBankName());
                    bankDetails.put("bankSwiftCode", vendor.getBankSwiftCode());
                    bankDetails.put("bankIban", vendor.getBankIban());
                    map.put("bankDetails", bankDetails);
                }
                
                return map;
            })
            .filter(payout -> {
                // Only include MoR vendors
                Optional<Vendor> vendorOpt = vendorRepository.findById((Long) payout.get("vendorId"));
                return vendorOpt.isPresent() && vendorOpt.get().getPayoutModel() == PayoutModel.MOR;
            })
            .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * Gets all MoR vendors with their balances (admin only)
     * @return List of vendors with balance information
     */
    public List<Map<String, Object>> getAllMoRVendorsWithBalances() {
        List<Vendor> morVendors = vendorRepository.findByPayoutModel(PayoutModel.MOR);
        
        return morVendors.stream()
            .map(vendor -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", vendor.getId());
                map.put("username", vendor.getUsername());
                map.put("email", vendor.getEmail());
                map.put("country", vendor.getCountry());
                map.put("balance", vendor.getBalance() != null ? vendor.getBalance() : java.math.BigDecimal.ZERO);
                map.put("currency", "EUR"); // Default currency
                map.put("hasBankDetails", vendor.getBankAccountNumber() != null && !vendor.getBankAccountNumber().isEmpty());
                
                // Count pending payouts
                List<VendorPayout> pendingPayouts = payoutRepository.findByVendorIdAndStatusOrderByRequestedAtDesc(
                    vendor.getId(), "PENDING");
                map.put("pendingPayoutsCount", pendingPayouts.size());
                
                // Calculate total pending payout amount
                java.math.BigDecimal totalPending = pendingPayouts.stream()
                    .map(VendorPayout::getAmount)
                    .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
                map.put("totalPendingPayouts", totalPending);
                
                return map;
            })
            .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * Gets all order payments for MoR vendors (admin only)
     * @param fromDate Optional start date filter
     * @param toDate Optional end date filter
     * @return List of order payment transactions
     */
    public List<Map<String, Object>> getAllMoROrderPayments(
            java.time.OffsetDateTime fromDate, 
            java.time.OffsetDateTime toDate) {
        
        List<VendorBalanceTransaction> transactions = balanceTransactionRepository.findByTypeOrderByCreatedAtDesc("ORDER_PAYMENT");
        
        return transactions.stream()
            .filter(txn -> {
                // Filter by MoR vendors
                Optional<Vendor> vendorOpt = vendorRepository.findById(txn.getVendorId());
                if (vendorOpt.isEmpty() || vendorOpt.get().getPayoutModel() != PayoutModel.MOR) {
                    return false;
                }
                
                // Filter by date range if provided
                if (fromDate != null && txn.getCreatedAt().isBefore(fromDate)) {
                    return false;
                }
                if (toDate != null && txn.getCreatedAt().isAfter(toDate)) {
                    return false;
                }
                
                return true;
            })
            .map(txn -> {
                Optional<Vendor> vendorOpt = vendorRepository.findById(txn.getVendorId());
                Vendor vendor = vendorOpt.orElse(null);
                
                Map<String, Object> map = new HashMap<>();
                map.put("id", txn.getId());
                map.put("vendorId", txn.getVendorId());
                map.put("vendorName", vendor != null ? vendor.getUsername() : "Unknown");
                map.put("vendorEmail", vendor != null ? vendor.getEmail() : "Unknown");
                map.put("orderId", txn.getOrderId());
                map.put("amount", txn.getAmount());
                map.put("currency", txn.getCurrency());
                map.put("description", txn.getDescription());
                map.put("createdAt", txn.getCreatedAt());
                return map;
            })
            .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * Gets payout summary for MoR vendors (admin only)
     * @return Summary statistics
     */
    public Map<String, Object> getMoRPayoutSummary() {
        List<VendorPayout> allPayouts = payoutRepository.findAllByOrderByRequestedAtDesc();
        
        // Filter to only MoR vendors
        List<VendorPayout> morPayouts = allPayouts.stream()
            .filter(payout -> {
                Optional<Vendor> vendorOpt = vendorRepository.findById(payout.getVendorId());
                return vendorOpt.isPresent() && vendorOpt.get().getPayoutModel() == PayoutModel.MOR;
            })
            .collect(java.util.stream.Collectors.toList());
        
        Map<String, Object> summary = new HashMap<>();
        
        // Count by status
        long pendingCount = morPayouts.stream().filter(p -> "PENDING".equals(p.getStatus())).count();
        long processingCount = morPayouts.stream().filter(p -> "PROCESSING".equals(p.getStatus())).count();
        long completedCount = morPayouts.stream().filter(p -> "COMPLETED".equals(p.getStatus())).count();
        long failedCount = morPayouts.stream().filter(p -> "FAILED".equals(p.getStatus())).count();
        
        // Calculate totals by status
        java.math.BigDecimal pendingTotal = morPayouts.stream()
            .filter(p -> "PENDING".equals(p.getStatus()))
            .map(VendorPayout::getAmount)
            .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        
        java.math.BigDecimal processingTotal = morPayouts.stream()
            .filter(p -> "PROCESSING".equals(p.getStatus()))
            .map(VendorPayout::getAmount)
            .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        
        summary.put("pendingCount", pendingCount);
        summary.put("processingCount", processingCount);
        summary.put("completedCount", completedCount);
        summary.put("failedCount", failedCount);
        summary.put("pendingTotal", pendingTotal);
        summary.put("processingTotal", processingTotal);
        summary.put("totalPendingAmount", pendingTotal.add(processingTotal));
        
        return summary;
    }
    
    /**
     * Updates payout status (admin only)
     * @param payoutId Payout ID
     * @param status New status
     * @param transactionReference Optional transaction reference
     * @param notes Optional admin notes
     */
    @org.springframework.transaction.annotation.Transactional
    public void updatePayoutStatus(Long payoutId, String status, String transactionReference, String notes) {
        Optional<VendorPayout> payoutOpt = payoutRepository.findById(payoutId);
        if (payoutOpt.isEmpty()) {
            throw new RuntimeException("Payout not found: " + payoutId);
        }
        
        VendorPayout payout = payoutOpt.get();
        
        // Validate status
        if (!java.util.Arrays.asList("PENDING", "PROCESSING", "COMPLETED", "FAILED").contains(status)) {
            throw new RuntimeException("Invalid payout status: " + status);
        }
        
        payout.setStatus(status);
        
        if (transactionReference != null && !transactionReference.isEmpty()) {
            payout.setTransactionReference(transactionReference);
        }
        
        if (notes != null && !notes.isEmpty()) {
            payout.setNotes(notes);
        }
        
        // Set processed date if completing or failing
        if ("COMPLETED".equals(status) || "FAILED".equals(status)) {
            if (payout.getProcessedAt() == null) {
                payout.setProcessedAt(java.time.OffsetDateTime.now());
            }
        }
        
        payoutRepository.save(payout);
        logger.info("Updated payout {} status to {}", payoutId, status);
    }
    
    /**
     * Gets all payouts for a specific vendor (admin only)
     * @param vendorId Vendor ID
     * @return List of payouts
     */
    public List<Map<String, Object>> getVendorPayouts(Long vendorId) {
        Optional<Vendor> vendorOpt = vendorRepository.findById(vendorId);
        if (vendorOpt.isEmpty()) {
            throw new RuntimeException("Vendor not found: " + vendorId);
        }
        
        Vendor vendor = vendorOpt.get();
        if (vendor.getPayoutModel() != PayoutModel.MOR) {
            throw new RuntimeException("Vendor is not using MoR model");
        }
        
        List<VendorPayout> payouts = payoutRepository.findByVendorIdOrderByRequestedAtDesc(vendorId);
        
        return payouts.stream()
            .map(p -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", p.getId());
                map.put("vendorId", p.getVendorId());
                map.put("amount", p.getAmount());
                map.put("currency", p.getCurrency());
                map.put("method", p.getMethod());
                map.put("status", p.getStatus());
                map.put("requestedAt", p.getRequestedAt());
                map.put("processedAt", p.getProcessedAt());
                map.put("transactionReference", p.getTransactionReference());
                map.put("notes", p.getNotes());
                return map;
            })
            .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * Validates that pickup date + pickup end time is in the future (ahead of current time).
     * Throws IllegalArgumentException if the pickup time is in the past.
     * This validation is performed synchronously before publishing to Kafka to return errors immediately to frontend.
     * 
     * @param pickupDate The pickup date
     * @param pickupEndTime The pickup end time
     * @param latitude Vendor's latitude for timezone detection
     * @param longitude Vendor's longitude for timezone detection
     * @param operation The operation being performed ("create" or "update") for error messages
     * @throws IllegalArgumentException if pickup date + end time is in the past
     */
    private void validatePickupTimeInFuture(LocalDate pickupDate, LocalTime pickupEndTime, Double latitude, Double longitude, String operation) {
        if (pickupDate == null || pickupEndTime == null) {
            // If either is null, skip validation (will be handled in offer-service)
            return;
        }
        
        if (latitude == null || longitude == null) {
            logger.warn("Vendor coordinates are null, cannot validate pickup time. Skipping validation.");
            return;
        }
        
        // Get vendor's timezone from coordinates
        ZoneId vendorZone = timeZoneDetectionService.getZoneId(latitude, longitude);
        
        // Combine pickup date + end time in vendor's timezone
        OffsetDateTime pickupEndDateTime = pickupDate.atTime(pickupEndTime)
            .atZone(vendorZone)
            .toOffsetDateTime();
        
        // Get current time in vendor's timezone
        OffsetDateTime now = OffsetDateTime.now(vendorZone);
        
        // Check if pickup end time is in the past
        if (pickupEndDateTime.isBefore(now) || pickupEndDateTime.isEqual(now)) {
            String errorMessage = String.format(
                "Cannot %s offer: Pickup end time (%s %s) must be in the future. Current time in vendor timezone: %s",
                operation,
                pickupDate,
                pickupEndTime,
                now.toLocalDate() + " " + now.toLocalTime()
            );
            logger.warn(errorMessage);
            throw new IllegalArgumentException(errorMessage);
        }
        
        logger.debug("Pickup time validation passed: {} {} is in the future (vendor timezone: {})", 
                    pickupDate, pickupEndTime, vendorZone);
    }

}
