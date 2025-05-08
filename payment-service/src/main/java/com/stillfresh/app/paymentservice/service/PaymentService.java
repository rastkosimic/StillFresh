package com.stillfresh.app.paymentservice.service;

import com.stillfresh.app.paymentservice.config.StripeProperties;
import com.stillfresh.app.paymentservice.dto.CardRegistrationRequest;
import com.stillfresh.app.paymentservice.dto.CardRegistrationResponse;
import com.stillfresh.app.paymentservice.dto.PaymentRequest;
import com.stillfresh.app.paymentservice.dto.PaymentResponse;
import com.stillfresh.app.paymentservice.model.PaymentUser;
import com.stillfresh.app.paymentservice.publisher.PaymentEventPublisher;
import com.stillfresh.app.paymentservice.repository.PaymentUserRepository;
import com.stillfresh.app.paymentservice.security.JwtUtil;
import com.stillfresh.app.sharedentities.enums.Currency;
import com.stillfresh.app.sharedentities.payment.events.PaymentFailureEvent;
import com.stillfresh.app.sharedentities.payment.events.PaymentRequestEvent;
import com.stillfresh.app.sharedentities.payment.events.PaymentSuccessEvent;
import com.stillfresh.app.sharedentities.payment.events.UpdatePaymentServiceEvent;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.PaymentIntent;
import com.stripe.model.PaymentMethod;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.PaymentMethodAttachParams;
import com.stripe.param.PaymentMethodListParams;

import jakarta.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class PaymentService {
	
    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);

    @Value("${stripe.api.key}")
    private String stripeApiKey;
    
    @Autowired
    private StripeProperties stripeProperties;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @Autowired
    private PaymentUserService paymentUserService;

    @Autowired
    private PaymentUserRepository paymentUserRepository;

    @Autowired
    private PaymentEventPublisher eventPublisher;
    
    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeProperties.getApiKey();
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

            return new CardRegistrationResponse(customerId, "Card registered successfully.");
        } catch (StripeException e) {
            logger.error("Stripe error during card registration: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to register card: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error during card registration: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to register card: " + e.getMessage());
        }
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

            // 🔹 Create a PaymentIntent with the fetched payment method
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(event.getAmount())  // Amount in cents
                    .setCurrency(event.getCurrency().getIsoCode())  // Use ISO currency code
                    .setCustomer(customerId)
                    .setPaymentMethod(paymentMethodId)  // Attach payment method
                    .setConfirm(true)
                    .setAutomaticPaymentMethods(
                        PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                            .setEnabled(true)
                            .setAllowRedirects(PaymentIntentCreateParams.AutomaticPaymentMethods.AllowRedirects.NEVER)
                            .build()
                    )
                    .build();

            PaymentIntent paymentIntent = PaymentIntent.create(params);
            logger.info("Payment successful for requestId: {}, PaymentIntent ID: {}, Status: {}", 
                        event.getRequestId(), paymentIntent.getId(), paymentIntent.getStatus());

            // 🔹 Publish Payment Success Event
            eventPublisher.publishPaymentSuccessEvent(new PaymentSuccessEvent(
                event.getRequestId(), event.getUserId(), event.getOfferId()
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

    public void processPaymentServiceUpdate(UpdatePaymentServiceEvent event) {
        logger.info("Received UpdatePaymentServiceEvent: oldUsername={}, newUsername={}", 
            event.getOldUsername(), event.getNewUsername());

        // ✅ Validate event
        if (event == null) {
            logger.warn("Received null UpdatePaymentServiceEvent. Skipping update.");
            return;
        }

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


}

