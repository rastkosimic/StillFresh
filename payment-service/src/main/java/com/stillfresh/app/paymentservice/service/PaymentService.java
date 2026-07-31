package com.stillfresh.app.paymentservice.service;

import com.stillfresh.app.paymentservice.dto.CardRegistrationRequest;
import com.stillfresh.app.paymentservice.dto.CardRegistrationResponse;
import com.stillfresh.app.paymentservice.dto.CustomerPaymentMethodDto;
import com.stillfresh.app.paymentservice.dto.PaymentRequest;
import com.stillfresh.app.paymentservice.dto.PaymentResponse;
import com.stillfresh.app.paymentservice.model.PaymentTransaction;
import com.stillfresh.app.paymentservice.model.PaymentUser;
import com.stillfresh.app.paymentservice.publisher.PaymentEventPublisher;
import com.stillfresh.app.paymentservice.repository.PaymentTransactionRepository;
import com.stillfresh.app.paymentservice.service.LedgerService;
import com.stillfresh.app.sharedentities.payment.events.OrderPaymentSettledEvent;
import com.stillfresh.app.sharedentities.payment.events.VendorPaymentInfoRequestEvent;
import com.stillfresh.app.sharedentities.payment.events.VendorPaymentInfoResponseEvent;
import com.stillfresh.app.paymentservice.repository.PaymentUserRepository;
import com.stillfresh.app.sharedentities.payment.events.PaymentFailureEvent;
import com.stillfresh.app.sharedentities.payment.events.PaymentRequestEvent;
import com.stillfresh.app.sharedentities.payment.events.PaymentSuccessEvent;
import com.stillfresh.app.sharedentities.payment.events.PaymentCapturedEvent;
import com.stillfresh.app.sharedentities.payment.events.UpdatePaymentServiceEvent;
import com.stripe.Stripe;
import com.stripe.exception.CardException;
import com.stripe.exception.StripeException;
import com.stillfresh.app.paymentservice.exception.PaymentMethodException;
import com.stripe.model.Customer;
import com.stripe.model.PaymentIntent;
import com.stripe.model.PaymentMethod;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.CustomerUpdateParams;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.PaymentIntentCaptureParams;
import com.stripe.param.PaymentIntentCancelParams;
import com.stripe.param.PaymentMethodAttachParams;
import com.stripe.param.PaymentMethodDetachParams;
import com.stripe.param.PaymentMethodListParams;

import jakarta.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
public class PaymentService {
	
    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);

    @Value("${stripe.apiKey}")
    private String stripeApiKey;
    
    
//    @Autowired
//    private JwtUtil jwtUtil;
    
    @Autowired
    private PaymentUserService paymentUserService;

    @Autowired
    private PaymentUserRepository paymentUserRepository;

    @Autowired
    private PaymentEventPublisher eventPublisher;
    
    // Store pending vendor payment info requests
    private final Map<String, CompletableFuture<com.stillfresh.app.sharedentities.payment.events.VendorPaymentInfoResponseEvent>> pendingVendorInfoRequests = new ConcurrentHashMap<>();

    /** TTL for vendor payment info cache: 10 minutes. */
    private static final long VENDOR_INFO_CACHE_TTL_MS = 10 * 60 * 1000;
    private final Map<Long, CachedVendorInfo> vendorPaymentInfoCache = new ConcurrentHashMap<>();

    /** TTL for payment methods list cache: 60 seconds. */
    private static final long PAYMENT_METHODS_CACHE_TTL_MS = 60 * 1000;
    private final Map<String, CachedPaymentMethods> paymentMethodsCache = new ConcurrentHashMap<>();
    
    @Autowired
    private StripeConnectService stripeConnectService;

    @Autowired
    private PaymentTransactionRepository paymentTransactionRepository;

    @Autowired
    private LedgerService ledgerService;

    @Autowired
    private PlatformSettingsService platformSettingsService;
    
    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeApiKey;
        logger.info("Stripe initialized (apiKeyConfigured={})",
            stripeApiKey != null && !stripeApiKey.isBlank());
    }


    // Register a Card
    public CardRegistrationResponse registerCard(CardRegistrationRequest request, Principal principal) {
        try {
            logger.info("Starting card registration process.");

            // ✅ Extract username from Principal
            String username = principal.getName();
//            Long userId = extractUserIdFromPrincipal(principal);  // Directly gets the claim from JWT
//            Long userId = extractUserIdFromContext();
//            logger.debug("Extracted userId from Principal: {}, username: {}", userId, username);

            // 🔹 Check if user exists in database
            Optional<PaymentUser> existingUser = paymentUserRepository.findByUsername(username);
            String customerId;

            if (existingUser.isPresent()) {
                customerId = existingUser.get().getStripeCustomerId();
                logger.info("User {} already has a Stripe customer ID: {}", username, customerId);
            } else {
                // 🔹 Create Stripe customer
                logger.info("Creating new Stripe customer for username: {}", username);
                CustomerCreateParams params = CustomerCreateParams.builder()
                        .setName(username)
                        .build();
                Customer customer = Customer.create(params);
                customerId = customer.getId();

                // 🔹 Save user
                PaymentUser paymentUser = new PaymentUser(username, customerId);
                paymentUserRepository.save(paymentUser);
                logger.info("New Stripe customer created with ID: {}", customerId);
            }

            // 🔹 Attach payment method
            logger.info("Retrieving payment method with ID: {}", request.getPaymentMethodId());
            PaymentMethod paymentMethod = PaymentMethod.retrieve(request.getPaymentMethodId());
            PaymentMethodAttachParams attachParams = PaymentMethodAttachParams.builder()
                    .setCustomer(customerId)
                    .build();
            paymentMethod.attach(attachParams);
            logger.info("Payment method attached successfully for customer ID: {}", customerId);

            evictPaymentMethodsCache(customerId);
            return new CardRegistrationResponse(customerId, "Card registered successfully.");
        } catch (CardException e) {
            // Handle card-specific errors (declined, insufficient funds, etc.)
            logger.warn("Card error during registration: code={}, message={}", e.getCode(), e.getMessage());
            String userMessage = getUserFriendlyCardErrorMessage(e);
            throw new PaymentMethodException(userMessage, e.getCode(), 400);
        } catch (StripeException e) {
            // Handle other Stripe errors
            logger.error("Stripe error during card registration: code={}, message={}", e.getCode(), e.getMessage());
            String userMessage = getUserFriendlyStripeErrorMessage(e);
            throw new PaymentMethodException(userMessage, e.getCode() != null ? e.getCode() : "STRIPE_ERROR", 400);
        } catch (Exception e) {
            logger.error("Unexpected error during card registration: {}", e.getMessage(), e);
            throw new PaymentMethodException("An unexpected error occurred while registering your card. Please try again.", "UNEXPECTED_ERROR", 500);
        }
    }

    /**
     * Converts Stripe CardException to user-friendly error message
     */
    private String getUserFriendlyCardErrorMessage(CardException e) {
        String code = e.getCode();
        if (code != null) {
            switch (code) {
                case "card_declined":
                    return "Your card was declined. Please try a different card or contact your bank.";
                case "insufficient_funds":
                    return "Your card has insufficient funds. Please use a different payment method.";
                case "expired_card":
                    return "Your card has expired. Please use a different card.";
                case "incorrect_cvc":
                    return "The card's security code is incorrect. Please check and try again.";
                case "incorrect_number":
                    return "The card number is incorrect. Please check and try again.";
                case "invalid_cvc":
                    return "The card's security code is invalid. Please check and try again.";
                case "invalid_expiry_month":
                    return "The card's expiration month is invalid. Please check and try again.";
                case "invalid_expiry_year":
                    return "The card's expiration year is invalid. Please check and try again.";
                case "invalid_number":
                    return "The card number is invalid. Please check and try again.";
                case "processing_error":
                    return "An error occurred while processing your card. Please try again.";
                default:
                    return e.getMessage() != null ? e.getMessage() : "Your card could not be processed. Please try a different card.";
            }
        }
        return e.getMessage() != null ? e.getMessage() : "Your card could not be processed. Please try a different card.";
    }

    /**
     * Converts Stripe StripeException to user-friendly error message
     */
    private String getUserFriendlyStripeErrorMessage(StripeException e) {
        String code = e.getCode();
        if (code != null) {
            switch (code) {
                case "resource_missing":
                    return "The payment method could not be found. Please try again.";
                case "api_key_expired":
                    return "Payment service configuration error. Please contact support.";
                case "rate_limit":
                    return "Too many requests. Please wait a moment and try again.";
                default:
                    return e.getMessage() != null ? e.getMessage() : "An error occurred while processing your request. Please try again.";
            }
        }
        return e.getMessage() != null ? e.getMessage() : "An error occurred while processing your request. Please try again.";
    }

    public PaymentResponse charge(PaymentRequest request, Principal principal) {
        try {

        	String username = principal.getName();
//            Long userId = extractUserIdFromContext();
//            logger.debug("Extracted userId from Principal: " + userId);

            // 🔹 Fetch Stripe Customer ID
            String customerId = paymentUserService.getCustomerIdByUsername(username);
            if (customerId == null) {
                return new PaymentResponse(null, "failed", "No Stripe customer ID found for this user.");
            }

            // 🔹 Create a PaymentIntent with return_url
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(request.getAmount())  // Amount in cents
                    .setCurrency(request.getCurrency())
                    .setCustomer(customerId)
                    .setPaymentMethod(request.getPaymentMethodId())  // Registered card
                    .setConfirm(true)
                    .setReturnUrl("https://yourapp.com/payment-confirmation")  // ✅ Add return URL
                    .setAutomaticPaymentMethods(
                        PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                            .setEnabled(true)
                            .build()
                    )
                    .build();

            PaymentIntent paymentIntent = PaymentIntent.create(params);

            return new PaymentResponse(paymentIntent.getId(), paymentIntent.getStatus(), "Payment successful.");
        } catch (StripeException e) {
            logger.error("Payment failed: {}", e.getMessage());
            return new PaymentResponse(null, "failed", "Payment failed: " + e.getMessage());
        }
    }

    public void processPaymentRequest(PaymentRequestEvent event) {
        try {
            logger.info("Processing payment request for userId: {}, username: {}, amount: {}", 
                        event.getUserId(), event.getUsername(), event.getAmount());

            // 🔹 Validate username exists in the event
            if (event.getUsername() == null || event.getUsername().isEmpty()) {
                throw new RuntimeException("Username is missing in the payment event.");
            }

            // 🔹 Fetch Stripe customer ID using the username
            String customerId = paymentUserService.getCustomerIdByUsername(event.getUsername());
            if (customerId == null) {
                throw new RuntimeException("No Stripe customer ID found for user: " + event.getUsername());
            }

            logger.info("Stripe customer ID retrieved: {}", customerId);

            // 🔹 Fetch all payment methods associated with the customer
            List<PaymentMethod> paymentMethods = PaymentMethod.list(
                PaymentMethodListParams.builder()
                    .setCustomer(customerId)
                    .setType(PaymentMethodListParams.Type.CARD)
                    .build()
            ).getData();

            if (paymentMethods.isEmpty()) {
                throw new RuntimeException("No payment methods found for user: " + event.getUsername());
            }

            // Use the first available payment method
            String paymentMethodId = paymentMethods.get(0).getId();
            logger.info("Using payment method ID: {}", paymentMethodId);

            // 🔹 Request vendor payment info: check cache first, then Kafka (payout model and Stripe account ID)
            String payoutModel = null;
            String stripeAccountId = event.getStripeAccountId(); // Use provided account ID if available
            boolean useStripeConnect = false;
            
            if (event.getVendorId() != null) {
                Long vendorId = event.getVendorId();
                CachedVendorInfo cached = vendorPaymentInfoCache.get(vendorId);
                if (cached != null && cached.expiresAt > System.currentTimeMillis()) {
                    payoutModel = cached.payoutModel;
                    if (stripeAccountId == null || stripeAccountId.isEmpty()) {
                        stripeAccountId = cached.stripeAccountId;
                    }
                    logger.debug("Using cached vendor payment info for vendor: {}", vendorId);
                } else {
                    if (cached != null) {
                        vendorPaymentInfoCache.remove(vendorId);
                    }
                    try {
                        logger.info("Requesting vendor payment info via Kafka for vendor: {}", vendorId);
                        VendorPaymentInfoResponseEvent vendorInfo = requestVendorPaymentInfo(vendorId);
                        
                        if (vendorInfo != null && vendorInfo.isSuccess()) {
                            payoutModel = vendorInfo.getPayoutModel();
                            if (stripeAccountId == null || stripeAccountId.isEmpty()) {
                                stripeAccountId = vendorInfo.getStripeAccountId();
                            }
                            vendorPaymentInfoCache.put(vendorId, new CachedVendorInfo(
                                    vendorInfo.getStripeAccountId(),
                                    vendorInfo.getPayoutModel(),
                                    System.currentTimeMillis() + VENDOR_INFO_CACHE_TTL_MS));
                            logger.info("Retrieved vendor payment info - vendorId: {}, payoutModel: {}, stripeAccountId: {}", 
                                       vendorId, payoutModel, stripeAccountId);
                        } else {
                            String errorMsg = vendorInfo != null ? vendorInfo.getErrorMessage() : "Unknown error";
                            logger.warn("Failed to get vendor payment info for vendor {}: {}. Will default to platform account.", 
                                       vendorId, errorMsg);
                        }
                    } catch (Exception e) {
                        logger.warn("Error or timeout requesting vendor payment info for vendor {}: {}. Payment will go to platform account only.", 
                                   vendorId, e.getMessage());
                    }
                }
            }
            
            // Determine if we should use Stripe Connect
            if ("MOR".equalsIgnoreCase(payoutModel)) {
                logger.info("Vendor {} uses MoR model. Payment will go to platform account, balance will be updated separately.", 
                           event.getVendorId());
                stripeAccountId = null; // Don't use Stripe Connect for MoR vendors
            }

            // 🔹 Check if vendor Stripe account is ready to receive payments (only for CONNECT model)
            if (stripeAccountId != null && !stripeAccountId.isEmpty() && !"MOR".equalsIgnoreCase(payoutModel)) {
                try {
                    logger.info("Checking if Stripe account {} is ready for vendor: {}", stripeAccountId, event.getVendorId());
                    boolean accountReady = stripeConnectService.isAccountReady(stripeAccountId);
                    if (!accountReady) {
                        logger.warn("Stripe account {} for vendor {} is not ready to receive payments. " +
                                   "Account may not have completed onboarding. Payment will go to platform account only.", 
                                   stripeAccountId, event.getVendorId());
                        stripeAccountId = null; // Don't use Connect transfer if account is not ready
                    } else {
                        logger.info("Stripe account {} for vendor {} is ready to receive payments.", 
                                   stripeAccountId, event.getVendorId());
                        useStripeConnect = true;
                    }
                } catch (Exception e) {
                    logger.error("Failed to check account readiness for vendor {} (account: {}): {}. " +
                               "Payment will go to platform account only.", 
                               event.getVendorId(), stripeAccountId, e.getMessage());
                    stripeAccountId = null; // Don't use Connect transfer if we can't verify readiness
                }
            }

            // 🔹 Calculate platform fee (in cents) using the runtime-customizable global fee
            double platformFeePercent = platformSettingsService.getFeePercent();
            long platformFeeAmount = calculatePlatformFee(event.getAmount(), platformFeePercent);
            long vendorAmount = event.getAmount() - platformFeeAmount;
            
            logger.info("Payment split - Total: {} cents, Platform fee ({}%): {} cents, Vendor amount: {} cents", 
                        event.getAmount(), platformFeePercent, platformFeeAmount, vendorAmount);

            // 🔹 Create PaymentIntent builder with manual capture (Too Good To Go style)
            // This places a hold on the card but doesn't charge immediately
            PaymentIntentCreateParams.Builder paramsBuilder = PaymentIntentCreateParams.builder()
                    .setAmount(event.getAmount())  // Total amount in cents (customer is charged this)
                    .setCurrency(event.getCurrency().getIsoCode())  // Use ISO currency code
                    .setCustomer(customerId)
                    .setPaymentMethod(paymentMethodId)  // Attach payment method
                    .setCaptureMethod(PaymentIntentCreateParams.CaptureMethod.MANUAL)  // Manual capture - hold funds, don't charge yet
                    .setConfirm(true)  // Confirm to authorize the payment (places hold)
                    .setAutomaticPaymentMethods(
                        PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                            .setEnabled(true)
                            .setAllowRedirects(PaymentIntentCreateParams.AutomaticPaymentMethods.AllowRedirects.NEVER)
                            .build()
                    );

            // 🔹 If vendor Stripe account is available and using CONNECT model, use Stripe Connect to split payment
            if (useStripeConnect && stripeAccountId != null && !stripeAccountId.isEmpty()) {
                logger.info("Using Stripe Connect for vendor: {} (account: {})", event.getVendorId(), stripeAccountId);
                
                paramsBuilder
                    .setApplicationFeeAmount(platformFeeAmount)  // Platform fee (stays in your account)
                    .setTransferData(
                        PaymentIntentCreateParams.TransferData.builder()
                            .setDestination(stripeAccountId)  // Transfer to vendor's Connect account
                            .build()
                    );
            } else if ("MOR".equalsIgnoreCase(payoutModel)) {
                logger.info("MoR vendor: {} - Payment goes to platform account, balance will be updated after payment.", event.getVendorId());
                // For MoR, payment goes to platform account (no Stripe Connect transfer)
            } else {
                logger.warn("No Stripe account ID available for vendor: {}. Payment will go to platform account only.", event.getVendorId());
            }

            PaymentIntentCreateParams params = paramsBuilder.build();
            PaymentIntent paymentIntent = PaymentIntent.create(params);
            
            // Check if payment was authorized successfully (status should be "requires_capture" for manual capture)
            String paymentIntentStatus = paymentIntent.getStatus();
            logger.info("PaymentIntent created for requestId: {}, PaymentIntent ID: {}, Status: {}", 
                        event.getRequestId(), paymentIntent.getId(), paymentIntentStatus);

            // Validate that the payment was authorized (not failed)
            if (!"requires_capture".equals(paymentIntentStatus) && !"succeeded".equals(paymentIntentStatus)) {
                throw new RuntimeException("Payment authorization failed. Status: " + paymentIntentStatus);
            }

            // 🔹 For MoR vendors, update their internal balance after successful payment
            // Note: MoR balance updates should also use Kafka events, but for now keeping this as-is
            // TODO: Refactor MoR balance updates to use Kafka events for consistency
            if ("MOR".equalsIgnoreCase(payoutModel) && event.getVendorId() != null) {
                logger.info("MoR vendor {} - Balance update should be handled via Kafka event (to be implemented)", 
                           event.getVendorId());
                // TODO: Publish MoR balance update event to Kafka instead of direct HTTP call
            }

            // 🔹 Save PaymentTransaction for ledger use at capture time
            try {
                PaymentTransaction tx = new PaymentTransaction();
                tx.setRequestId(event.getRequestId());
                tx.setPaymentIntentId(paymentIntent.getId());
                tx.setUserId(event.getUserId());
                tx.setVendorId(event.getVendorId());
                tx.setOfferId(event.getOfferId());
                tx.setGrossAmountCents(event.getAmount());
                tx.setPlatformFeeCents(platformFeeAmount);
                tx.setNetAmountCents(vendorAmount);
                tx.setFeePercentApplied(platformFeePercent);
                tx.setCurrency(event.getCurrency() != null ? event.getCurrency().getIsoCode() : "RSD");
                paymentTransactionRepository.save(tx);
                logger.debug("PaymentTransaction saved for requestId={}, paymentIntentId={}",
                             event.getRequestId(), paymentIntent.getId());
            } catch (Exception e) {
                logger.error("Failed to save PaymentTransaction for requestId={}: {}",
                             event.getRequestId(), e.getMessage(), e);
                // Non-fatal: payment has already succeeded; ledger can be reconciled manually
            }

            // 🔹 Publish Payment Success Event with PaymentIntent ID
            eventPublisher.publishPaymentSuccessEvent(new PaymentSuccessEvent(
                event.getRequestId(), event.getUserId(), event.getOfferId(), paymentIntent.getId()
            ));

        } catch (StripeException e) {
            logger.error("Payment failed for requestId: {}, reason: {}", event.getRequestId(), e.getMessage());

            // 🔹 Publish Payment Failure Event
            eventPublisher.publishPaymentFailureEvent(new PaymentFailureEvent(
                event.getRequestId(), event.getUserId(), event.getOfferId(), e.getMessage()
            ));
        } catch (Exception e) {
            logger.error("Unexpected error during payment processing: {}", e.getMessage(), e);
            eventPublisher.publishPaymentFailureEvent(new PaymentFailureEvent(
                event.getRequestId(), event.getUserId(), event.getOfferId(), e.getMessage()
            ));
        }
    }

    /**
     * Calculates platform fee based on percentage
     * @param totalAmount Total amount in cents
     * @param feePercent Platform fee percentage (e.g., 5.0 for 5%)
     * @return Platform fee amount in cents
     */
    private long calculatePlatformFee(long totalAmount, double feePercent) {
        double feeDecimal = feePercent / 100.0;
        double feeAmount = totalAmount * feeDecimal;
        return Math.round(feeAmount);
    }

    public void processPaymentServiceUpdate(UpdatePaymentServiceEvent event) {
        logger.info("Received UpdatePaymentServiceEvent: oldUsername={}, newUsername={}", 
            event.getOldUsername(), event.getNewUsername());

        if (event.getOldUsername() == null || event.getOldUsername().isEmpty()) {
            logger.warn("Old username is missing in UpdatePaymentServiceEvent. Cannot proceed.");
            return;
        }

        if (event.getNewUsername() == null || event.getNewUsername().isEmpty()) {
            logger.warn("New username is missing in UpdatePaymentServiceEvent. Cannot proceed.");
            return;
        }

        try {
            // ✅ Fetch PaymentUser by old username
            Optional<PaymentUser> optionalPaymentUser = paymentUserRepository.findByUsername(event.getOldUsername());

            if (optionalPaymentUser.isEmpty()) {
                logger.warn("No PaymentUser found with oldUsername: {}. Skipping update.", event.getOldUsername());
                return;
            }

            PaymentUser paymentUser = optionalPaymentUser.get();

            // ✅ Check if new username is the same as the old one
            if (paymentUser.getUsername().equals(event.getNewUsername())) {
                logger.info("New username is the same as the existing one. No update required.");
                return;
            }

            // ✅ Update username and save
            paymentUser.setUsername(event.getNewUsername());
            paymentUserRepository.save(paymentUser);
            logger.info("Successfully updated username for PaymentUser. Old: {}, New: {}", 
                event.getOldUsername(), event.getNewUsername());

        } catch (Exception e) {
            logger.error("Error updating PaymentUser username. Old: {}, New: {}, Error: {}", 
                event.getOldUsername(), event.getNewUsername(), e.getMessage(), e);
        }
    }





//	public Long extractUserIdFromContext() {
//	    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
//	    
//	    logger.info("Security Context Authentication: {}", authentication);
//
//	    if (authentication != null) {
//	        Object credentials = authentication.getCredentials();
//	        logger.info("Extracted Credentials: {}", credentials);
//
//	        if (credentials instanceof String jwt) {
//	            try {
//	                Long userId = jwtUtil.extractClaim(jwt, claims -> claims.get("userId", Long.class));
//	                logger.info("Extracted userId from token: {}", userId);
//	                return userId;
//	            } catch (Exception e) {
//	                logger.error("Error extracting user ID from token: {}", e.getMessage());
//	            }
//	        } else {
//	            logger.warn("JWT is not a String instance in credentials: {}", credentials);
//	        }
//	    } else {
//	        logger.warn("Authentication is null in SecurityContext.");
//	    }
//	    
//	    return null;
//	}

    /**
     * Requests vendor payment info (payout model and Stripe account ID) via Kafka
     * @param vendorId Vendor ID
     * @return VendorPaymentInfoResponseEvent with payout model and Stripe account ID
     * @throws Exception if request times out or fails
     */
    /** Public entry-point used by PayoutSchedulerService to fetch vendor IBAN at scheduling time. */
    public VendorPaymentInfoResponseEvent getVendorPaymentInfo(Long vendorId) throws Exception {
        return requestVendorPaymentInfo(vendorId);
    }

    private VendorPaymentInfoResponseEvent requestVendorPaymentInfo(Long vendorId) throws Exception {
        String requestId = UUID.randomUUID().toString();
        VendorPaymentInfoRequestEvent requestEvent = new VendorPaymentInfoRequestEvent(requestId, vendorId);
        
        CompletableFuture<VendorPaymentInfoResponseEvent> future = new CompletableFuture<>();
        pendingVendorInfoRequests.put(requestId, future);
        
        try {
            eventPublisher.publishVendorPaymentInfoRequest(requestEvent);
            logger.info("Published VendorPaymentInfoRequestEvent for vendor: {}, requestId: {}", vendorId, requestId);
            
            // Wait for response with timeout (10 seconds)
            VendorPaymentInfoResponseEvent response = future.get(10, TimeUnit.SECONDS);
            pendingVendorInfoRequests.remove(requestId);
            
            return response;
        } catch (java.util.concurrent.TimeoutException e) {
            pendingVendorInfoRequests.remove(requestId);
            logger.error("Timeout waiting for vendor payment info response for vendor: {}, requestId: {}", vendorId, requestId);
            throw new RuntimeException("Timeout waiting for vendor payment info: " + e.getMessage(), e);
        } catch (Exception e) {
            pendingVendorInfoRequests.remove(requestId);
            logger.error("Error requesting vendor payment info for vendor: {}, requestId: {}", vendorId, requestId, e);
            throw e;
        }
    }

    /**
     * Handles vendor payment info response from Kafka listener
     * @param response VendorPaymentInfoResponseEvent
     */
    public void handleVendorPaymentInfoResponse(VendorPaymentInfoResponseEvent response) {
        logger.info("Handling vendor payment info response: requestId={}, vendorId={}, success={}", 
                   response.getRequestId(), response.getVendorId(), response.isSuccess());
        
        CompletableFuture<VendorPaymentInfoResponseEvent> future = pendingVendorInfoRequests.remove(response.getRequestId());
        if (future != null) {
            future.complete(response);
            logger.info("Completed future for vendor payment info request: {}", response.getRequestId());
        } else {
            logger.warn("No pending future found for vendor payment info request: {}", response.getRequestId());
        }
    }

    // ========== Customer Payment Method Management ==========

    /**
     * Lists all payment methods (cards and bank accounts) for the authenticated customer
     */
    public List<CustomerPaymentMethodDto> getCustomerPaymentMethods(Principal principal) {
        try {
            String username = principal.getName();
            String customerId = paymentUserService.getCustomerIdByUsername(username);
            
            // No Stripe customer yet: return empty list. Customer is created when user adds first payment method (registerCard/registerBankAccount).
            if (customerId == null) {
                logger.debug("No PaymentUser found for username: {}. Returning empty payment methods list.", username);
                return new ArrayList<>();
            }

            CachedPaymentMethods cached = paymentMethodsCache.get(customerId);
            if (cached != null && cached.expiresAt > System.currentTimeMillis()) {
                logger.debug("Returning cached payment methods for customer: {}", customerId);
                return new ArrayList<>(cached.list);
            }
            if (cached != null) {
                paymentMethodsCache.remove(customerId);
            }

            logger.info("Retrieving payment methods for customer: {}", customerId);
            
            // Default payment method: use stored value to avoid Customer.retrieve; fallback to Stripe if not set
            String defaultPaymentMethodId = paymentUserService.getDefaultPaymentMethodId(username);
            if (defaultPaymentMethodId == null) {
                Customer customer = Customer.retrieve(customerId);
                defaultPaymentMethodId = customer.getInvoiceSettings() != null 
                    ? customer.getInvoiceSettings().getDefaultPaymentMethod() 
                    : null;
            }
            
            // Get all payment methods (cards and bank accounts)
            List<PaymentMethod> paymentMethods = PaymentMethod.list(
                PaymentMethodListParams.builder()
                    .setCustomer(customerId)
                    .build()
            ).getData();

            List<CustomerPaymentMethodDto> result = new ArrayList<>();
            for (PaymentMethod pm : paymentMethods) {
                CustomerPaymentMethodDto dto = convertPaymentMethodToDto(pm);
                dto.setIsDefault(pm.getId().equals(defaultPaymentMethodId));
                result.add(dto);
            }

            paymentMethodsCache.put(customerId, new CachedPaymentMethods(
                    new ArrayList<>(result),
                    System.currentTimeMillis() + PAYMENT_METHODS_CACHE_TTL_MS));
            logger.info("Retrieved {} payment methods for customer: {}", result.size(), customerId);
            return result;
        } catch (StripeException e) {
            logger.error("Stripe error retrieving payment methods: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve payment methods: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Error retrieving payment methods: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve payment methods: " + e.getMessage(), e);
        }
    }

    /**
     * Gets a specific payment method by ID
     */
    public CustomerPaymentMethodDto getCustomerPaymentMethod(String paymentMethodId, Principal principal) {
        try {
            String username = principal.getName();
            String customerId = paymentUserService.getCustomerIdByUsername(username);
            
            // ✅ Handle new customers who don't have a PaymentUser record yet
            if (customerId == null) {
                logger.info("No PaymentUser found for username: {}. Customer has no payment methods yet.", username);
                throw new RuntimeException("No payment methods found. Please add a payment method first.");
            }

            logger.info("Retrieving payment method {} for customer: {}", paymentMethodId, customerId);
            
            PaymentMethod paymentMethod = PaymentMethod.retrieve(paymentMethodId);
            
            // Verify the payment method belongs to this customer
            if (!customerId.equals(paymentMethod.getCustomer())) {
                throw new RuntimeException("Payment method does not belong to this customer");
            }

            // Default: use stored value to avoid Customer.retrieve; fallback to Stripe if not set
            String defaultPaymentMethodId = paymentUserService.getDefaultPaymentMethodId(username);
            if (defaultPaymentMethodId == null) {
                Customer customer = Customer.retrieve(customerId);
                defaultPaymentMethodId = customer.getInvoiceSettings() != null 
                    ? customer.getInvoiceSettings().getDefaultPaymentMethod() 
                    : null;
            }

            CustomerPaymentMethodDto dto = convertPaymentMethodToDto(paymentMethod);
            dto.setIsDefault(paymentMethodId.equals(defaultPaymentMethodId));
            
            return dto;
        } catch (StripeException e) {
            logger.error("Failed to retrieve payment method {}: {}", paymentMethodId, e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve payment method: " + e.getMessage());
        }
    }

    /**
     * Registers a bank account for the customer
     */
    public CustomerPaymentMethodDto registerBankAccount(String bankAccountToken, Principal principal) {
        try {
            String username = principal.getName();
            String customerId = paymentUserService.getCustomerIdByUsername(username);
            
            if (customerId == null) {
                // Create customer if doesn't exist
                CustomerCreateParams params = CustomerCreateParams.builder()
                        .setName(username)
                        .build();
                Customer customer = Customer.create(params);
                customerId = customer.getId();
                
                PaymentUser paymentUser = new PaymentUser(username, customerId);
                paymentUserRepository.save(paymentUser);
                logger.info("Created new Stripe customer: {}", customerId);
            }

            logger.info("Registering bank account for customer: {}", customerId);
            
            // Retrieve the payment method (created by Stripe.js/Elements)
            PaymentMethod paymentMethod = PaymentMethod.retrieve(bankAccountToken);
            
            // Verify it's a bank account
            if (!"us_bank_account".equals(paymentMethod.getType())) {
                throw new RuntimeException("Payment method is not a bank account");
            }

            // Attach to customer
            PaymentMethodAttachParams attachParams = PaymentMethodAttachParams.builder()
                    .setCustomer(customerId)
                    .build();
            paymentMethod.attach(attachParams);
            
            logger.info("Bank account attached successfully for customer: {}", customerId);
            
            evictPaymentMethodsCache(customerId);
            CustomerPaymentMethodDto dto = convertPaymentMethodToDto(paymentMethod);
            dto.setIsDefault(false); // New payment method is not default
            
            return dto;
        } catch (CardException e) {
            logger.warn("Card error during bank account registration: code={}, message={}", e.getCode(), e.getMessage());
            String userMessage = getUserFriendlyCardErrorMessage(e);
            throw new PaymentMethodException(userMessage, e.getCode(), 400);
        } catch (StripeException e) {
            logger.error("Stripe error during bank account registration: code={}, message={}", e.getCode(), e.getMessage());
            String userMessage = getUserFriendlyStripeErrorMessage(e);
            throw new PaymentMethodException(userMessage, e.getCode() != null ? e.getCode() : "STRIPE_ERROR", 400);
        } catch (Exception e) {
            logger.error("Unexpected error during bank account registration: {}", e.getMessage(), e);
            throw new PaymentMethodException("An unexpected error occurred while registering your bank account. Please try again.", "UNEXPECTED_ERROR", 500);
        }
    }

    /**
     * Sets a payment method as default for the customer
     */
    public CustomerPaymentMethodDto setDefaultPaymentMethod(String paymentMethodId, Principal principal) {
        try {
            String username = principal.getName();
            String customerId = paymentUserService.getCustomerIdByUsername(username);
            
            if (customerId == null) {
                throw new RuntimeException("No Stripe customer found for user: " + username);
            }

            logger.info("Setting payment method {} as default for customer: {}", paymentMethodId, customerId);
            
            // Verify the payment method belongs to this customer
            PaymentMethod paymentMethod = PaymentMethod.retrieve(paymentMethodId);
            if (!customerId.equals(paymentMethod.getCustomer())) {
                throw new RuntimeException("Payment method does not belong to this customer");
            }

            // Update customer's default payment method in Stripe
            CustomerUpdateParams updateParams = CustomerUpdateParams.builder()
                    .setInvoiceSettings(
                        CustomerUpdateParams.InvoiceSettings.builder()
                            .setDefaultPaymentMethod(paymentMethodId)
                            .build()
                    )
                    .build();
            Customer customer = Customer.retrieve(customerId);
            customer.update(updateParams);
            
            paymentUserService.updateDefaultPaymentMethod(username, paymentMethodId);
            logger.info("Payment method {} set as default for customer: {}", paymentMethodId, customerId);
            
            evictPaymentMethodsCache(customerId);
            CustomerPaymentMethodDto dto = convertPaymentMethodToDto(paymentMethod);
            dto.setIsDefault(true);
            
            return dto;
        } catch (StripeException e) {
            logger.error("Failed to set default payment method: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to set default payment method: " + e.getMessage());
        }
    }

    /**
     * Deletes a payment method
     */
    public void deletePaymentMethod(String paymentMethodId, Principal principal) {
        try {
            String username = principal.getName();
            String customerId = paymentUserService.getCustomerIdByUsername(username);
            
            if (customerId == null) {
                throw new RuntimeException("No Stripe customer found for user: " + username);
            }

            logger.info("Deleting payment method {} for customer: {}", paymentMethodId, customerId);
            
            // Verify the payment method belongs to this customer
            PaymentMethod paymentMethod = PaymentMethod.retrieve(paymentMethodId);
            if (!customerId.equals(paymentMethod.getCustomer())) {
                throw new RuntimeException("Payment method does not belong to this customer");
            }

            // Check if it's the default payment method
            Customer customer = Customer.retrieve(customerId);
            String defaultPaymentMethodId = customer.getInvoiceSettings() != null 
                ? customer.getInvoiceSettings().getDefaultPaymentMethod() 
                : null;
            
            if (paymentMethodId.equals(defaultPaymentMethodId)) {
                // Remove default payment method from customer in Stripe and in DB
                CustomerUpdateParams updateParams = CustomerUpdateParams.builder()
                        .setInvoiceSettings(
                            CustomerUpdateParams.InvoiceSettings.builder()
                                .setDefaultPaymentMethod((String) null) // Clear default
                                .build()
                        )
                        .build();
                customer.update(updateParams);
                paymentUserService.updateDefaultPaymentMethod(username, null);
                logger.info("Removed default payment method from customer: {}", customerId);
            }

            // Detach and delete the payment method
            PaymentMethodDetachParams detachParams = PaymentMethodDetachParams.builder().build();
            paymentMethod.detach(detachParams);
            
            evictPaymentMethodsCache(customerId);
            logger.info("Payment method {} deleted successfully for customer: {}", paymentMethodId, customerId);
        } catch (StripeException e) {
            logger.error("Failed to delete payment method: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to delete payment method: " + e.getMessage());
        }
    }

    /**
     * Captures a PaymentIntent (charges the customer) - called when order is picked up
     * This implements the Too Good To Go style payment flow
     * @param paymentIntentId The Stripe PaymentIntent ID
     * @return PaymentIntent with updated status
     * @throws StripeException if capture fails
     */
    public PaymentIntent capturePaymentIntent(String paymentIntentId) throws StripeException {
        logger.info("Capturing PaymentIntent: {}", paymentIntentId);
        
        try {
            PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentIntentId);
            
            // Check if PaymentIntent is in a state that can be captured
            String status = paymentIntent.getStatus();
            if (!"requires_capture".equals(status)) {
                throw new RuntimeException("PaymentIntent cannot be captured. Current status: " + status);
            }
            
            // Capture the PaymentIntent
            PaymentIntentCaptureParams captureParams = PaymentIntentCaptureParams.builder().build();
            PaymentIntent capturedIntent = paymentIntent.capture(captureParams);
            
            logger.info("PaymentIntent {} captured successfully. New status: {}", 
                        paymentIntentId, capturedIntent.getStatus());
            
            // Publish PaymentCapturedEvent to notify order service to update order status to COMPLETED
            if ("succeeded".equals(capturedIntent.getStatus())) {
                eventPublisher.publishPaymentCapturedEvent(
                    new PaymentCapturedEvent(paymentIntentId, capturedIntent.getStatus())
                );
                logger.info("Published PaymentCapturedEvent for PaymentIntent {}", paymentIntentId);

                // Write ledger entries and fire OrderPaymentSettledEvent
                try {
                    paymentTransactionRepository.findByPaymentIntentId(paymentIntentId).ifPresentOrElse(
                        tx -> {
                            ledgerService.writePaymentLedger(tx);
                            eventPublisher.publishOrderPaymentSettledEvent(new OrderPaymentSettledEvent(
                                paymentIntentId,
                                tx.getUserId(),
                                tx.getVendorId(),
                                tx.getOfferId(),
                                tx.getGrossAmountCents(),
                                tx.getPlatformFeeCents(),
                                tx.getNetAmountCents(),
                                tx.getCurrency(),
                                tx.getFeePercentApplied()
                            ));
                        },
                        () -> logger.warn("No PaymentTransaction found for paymentIntentId={}; ledger skipped. "
                                        + "Manual reconciliation may be required.", paymentIntentId)
                    );
                } catch (Exception e) {
                    logger.error("Failed to write ledger for paymentIntentId={}: {}", paymentIntentId, e.getMessage(), e);
                }
            }
            
            return capturedIntent;
        } catch (StripeException e) {
            logger.error("Failed to capture PaymentIntent {}: {}", paymentIntentId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Cancels a PaymentIntent (releases the hold) - called when order is cancelled
     * This implements the Too Good To Go style payment flow
     * @param paymentIntentId The Stripe PaymentIntent ID
     * @return PaymentIntent with updated status
     * @throws StripeException if cancellation fails
     */
    public PaymentIntent cancelPaymentIntent(String paymentIntentId) throws StripeException {
        logger.info("Cancelling PaymentIntent: {}", paymentIntentId);
        
        try {
            PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentIntentId);
            
            // Check if PaymentIntent can be cancelled
            String status = paymentIntent.getStatus();
            if ("succeeded".equals(status) || "canceled".equals(status)) {
                logger.warn("PaymentIntent {} is already in status: {}. Cannot cancel.", paymentIntentId, status);
                return paymentIntent;
            }
            
            // Cancel the PaymentIntent
            PaymentIntentCancelParams cancelParams = PaymentIntentCancelParams.builder().build();
            PaymentIntent cancelledIntent = paymentIntent.cancel(cancelParams);
            
            logger.info("PaymentIntent {} cancelled successfully. New status: {}", 
                        paymentIntentId, cancelledIntent.getStatus());
            
            return cancelledIntent;
        } catch (StripeException e) {
            logger.error("Failed to cancel PaymentIntent {}: {}", paymentIntentId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Converts Stripe PaymentMethod to DTO
     */
    private CustomerPaymentMethodDto convertPaymentMethodToDto(PaymentMethod paymentMethod) {
        CustomerPaymentMethodDto dto = new CustomerPaymentMethodDto();
        dto.setPaymentMethodId(paymentMethod.getId());
        dto.setType(paymentMethod.getType());
        
        if ("card".equals(paymentMethod.getType()) && paymentMethod.getCard() != null) {
            dto.setCardBrand(paymentMethod.getCard().getBrand());
            dto.setCardLast4(paymentMethod.getCard().getLast4());
            dto.setCardExpMonth(paymentMethod.getCard().getExpMonth());
            dto.setCardExpYear(paymentMethod.getCard().getExpYear());
            dto.setCardFunding(paymentMethod.getCard().getFunding());
            dto.setCountry(paymentMethod.getCard().getCountry());
        } else if ("us_bank_account".equals(paymentMethod.getType()) && paymentMethod.getUsBankAccount() != null) {
            dto.setBankAccountType(paymentMethod.getUsBankAccount().getAccountType());
            dto.setBankAccountLast4(paymentMethod.getUsBankAccount().getLast4());
            dto.setBankName(paymentMethod.getUsBankAccount().getBankName());
            dto.setBankAccountHolderType(paymentMethod.getUsBankAccount().getAccountHolderType());
            // Note: Status is not directly available on PaymentMethod.UsBankAccount
            // It's typically managed through verification flow
            dto.setBankAccountStatus(null);
        }
        
        return dto;
    }

    /** Cached vendor payment info (Stripe account ID + payout model) with expiry. */
    private static final class CachedVendorInfo {
        final String stripeAccountId;
        final String payoutModel;
        final long expiresAt;

        CachedVendorInfo(String stripeAccountId, String payoutModel, long expiresAt) {
            this.stripeAccountId = stripeAccountId;
            this.payoutModel = payoutModel;
            this.expiresAt = expiresAt;
        }
    }

    /** Cached list of payment methods for a customer with expiry. */
    private static final class CachedPaymentMethods {
        final List<CustomerPaymentMethodDto> list;
        final long expiresAt;

        CachedPaymentMethods(List<CustomerPaymentMethodDto> list, long expiresAt) {
            this.list = list;
            this.expiresAt = expiresAt;
        }
    }

    /** Evicts cached payment methods for the given Stripe customer (call after add/remove/setDefault). */
    private void evictPaymentMethodsCache(String customerId) {
        if (customerId != null) {
            paymentMethodsCache.remove(customerId);
            logger.debug("Evicted payment methods cache for customer: {}", customerId);
        }
    }
}

