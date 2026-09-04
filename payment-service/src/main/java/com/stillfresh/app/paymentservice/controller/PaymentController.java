package com.stillfresh.app.paymentservice.controller;

import com.stillfresh.app.paymentservice.dto.CardRegistrationRequest;
import com.stillfresh.app.paymentservice.dto.CardRegistrationResponse;
import com.stillfresh.app.paymentservice.dto.CustomerPaymentMethodDto;
import com.stillfresh.app.paymentservice.dto.PaymentRequest;
import com.stillfresh.app.paymentservice.dto.PaymentResponse;
import com.stillfresh.app.paymentservice.exception.PaymentMethodException;
import com.stillfresh.app.paymentservice.provider.PaymentProviderRouter;
import com.stillfresh.app.paymentservice.repository.PaymentTransactionRepository;
import com.stillfresh.app.paymentservice.security.CallerContext;
import com.stillfresh.app.paymentservice.service.PaymentService;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

@RestController
@RequestMapping("/payment")
@Tag(name = "Payment", description = "Payment and payment method management APIs")
@SecurityRequirement(name = "bearerAuth")
public class PaymentController {

    private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PaymentProviderRouter paymentProviderRouter;

    @Autowired
    private PaymentTransactionRepository paymentTransactionRepository;

    @Autowired
    private CallerContext callerContext;

    private static boolean isVendorOrAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(r -> r.equals("ROLE_VENDOR") || r.equals("ROLE_VENDOR_ADMIN")
                            || r.equals("ROLE_ADMIN")  || r.equals("ROLE_SUPER_ADMIN"));
    }

    /**
     * Checks that the caller is entitled to act on the given authorization reference.
     *
     * <p>The role check above only established that the caller is <em>some</em> vendor. Without
     * this, any vendor could capture or void the authorization belonging to another vendor's
     * order — taking a customer's money or releasing a competitor's hold.
     *
     * <p>Both providers persist a {@code PaymentTransaction} keyed by the reference (Stripe
     * PaymentIntent ID or AllSecure preauth UUID), so the same lookup covers both. An unknown
     * reference is refused for vendors rather than passed through to the provider.
     */
    private boolean callerOwnsPayment(String paymentReference) {
        if (callerContext.isAdmin()) {
            return true;
        }
        Long callerVendorId = callerContext.vendorId();
        if (callerVendorId == null) {
            return false;
        }
        return paymentTransactionRepository.findByPaymentIntentId(paymentReference)
                .map(tx -> callerVendorId.equals(tx.getVendorId()))
                .orElseGet(() -> {
                    logger.warn("Rejected action on unknown payment reference {} by vendorId {}",
                            paymentReference, callerVendorId);
                    return false;
                });
    }
    
    //Register a Card
    @PostMapping("/register-card")
    @Operation(summary = "Register a card", description = "Registers a new card payment method for the authenticated customer")
    public ResponseEntity<?> registerCard(@RequestBody CardRegistrationRequest request, Principal principal) {
        try {
            CardRegistrationResponse response = paymentService.registerCard(request, principal);
            return ResponseEntity.ok(response);
        } catch (PaymentMethodException e) {
            return ResponseEntity.status(e.getHttpStatus())
                    .body(Map.of("error", e.getMessage(), "errorCode", e.getErrorCode()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An unexpected error occurred. Please try again."));
        }
    }

    //Make a One-Time Payment
    @PostMapping("/charge")
    @Operation(summary = "Make a payment", description = "Processes a one-time payment using a registered payment method")
    public ResponseEntity<PaymentResponse> charge(@RequestBody PaymentRequest request, Principal principal) {
        PaymentResponse response = paymentService.charge(request, principal);
        return ResponseEntity.ok(response);
    }

    // ========== Payment Method Management ==========

    /**
     * Lists all payment methods (cards and bank accounts) for the authenticated customer
     */
    @GetMapping("/payment-methods")
    @Operation(
        summary = "List payment methods",
        description = "Retrieves all payment methods (cards and bank accounts) for the authenticated customer"
    )
    public ResponseEntity<List<CustomerPaymentMethodDto>> getPaymentMethods(Principal principal) {
        try {
            List<CustomerPaymentMethodDto> paymentMethods = paymentService.getCustomerPaymentMethods(principal);
            return ResponseEntity.ok(paymentMethods);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Gets a specific payment method by ID
     */
    @GetMapping("/payment-methods/{paymentMethodId}")
    @Operation(
        summary = "Get payment method",
        description = "Retrieves details of a specific payment method by ID"
    )
    public ResponseEntity<CustomerPaymentMethodDto> getPaymentMethod(
            @Parameter(description = "Payment method ID") @PathVariable String paymentMethodId,
            Principal principal) {
        try {
            CustomerPaymentMethodDto paymentMethod = paymentService.getCustomerPaymentMethod(paymentMethodId, principal);
            return ResponseEntity.ok(paymentMethod);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(null);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Registers a bank account for the customer
     */
    @PostMapping("/register-bank-account")
    @Operation(
        summary = "Register bank account",
        description = "Registers a new bank account payment method for the authenticated customer. Requires a token from Stripe.js/Elements."
    )
    public ResponseEntity<?> registerBankAccount(
            @Parameter(description = "Bank account token from Stripe.js/Elements") @RequestParam("bankAccountToken") String bankAccountToken,
            Principal principal) {
        try {
            CustomerPaymentMethodDto bankAccount = paymentService.registerBankAccount(bankAccountToken, principal);
            return ResponseEntity.ok(bankAccount);
        } catch (PaymentMethodException e) {
            return ResponseEntity.status(e.getHttpStatus())
                    .body(Map.of("error", e.getMessage(), "errorCode", e.getErrorCode()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An unexpected error occurred. Please try again."));
        }
    }

    /**
     * Sets a payment method as default
     */
    @PutMapping("/payment-methods/{paymentMethodId}/default")
    @Operation(
        summary = "Set default payment method",
        description = "Sets a payment method as the default for the authenticated customer"
    )
    public ResponseEntity<CustomerPaymentMethodDto> setDefaultPaymentMethod(
            @Parameter(description = "Payment method ID to set as default") @PathVariable String paymentMethodId,
            Principal principal) {
        try {
            CustomerPaymentMethodDto paymentMethod = paymentService.setDefaultPaymentMethod(paymentMethodId, principal);
            return ResponseEntity.ok(paymentMethod);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(null);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Deletes a payment method
     */
    @DeleteMapping("/payment-methods/{paymentMethodId}")
    @Operation(
        summary = "Delete payment method",
        description = "Deletes a payment method. If it's the default payment method, the default will be cleared."
    )
    public ResponseEntity<Map<String, Object>> deletePaymentMethod(
            @Parameter(description = "Payment method ID to delete") @PathVariable String paymentMethodId,
            Principal principal) {
        try {
            paymentService.deletePaymentMethod(paymentMethodId, principal);
            return ResponseEntity.ok(Map.of("success", true, "message", "Payment method deleted successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Failed to delete payment method"));
        }
    }

    // ========== Too Good To Go Style Payment Flow ==========

    /**
     * Captures a previously authorized payment (charges the customer) — vendor/admin at pickup.
     * Provider-neutral: routes through the active {@link PaymentProviderRouter} so it works for both
     * Stripe (PaymentIntent {@code pi_...}) and AllSecure (preauth reference UUID).
     * Customers should call {@code PUT /orders/{id}/confirm-pickup} on order-service instead (customer JWT).
     */
    @PostMapping("/capture/{paymentIntentId}")
    @Operation(
        summary = "Capture payment",
        description = "Captures a previously authorized payment via the active provider (vendor or admin only). " +
                      "Customers must use PUT /orders/{orderId}/confirm-pickup on the order service so ownership is enforced."
    )
    public ResponseEntity<Map<String, Object>> capturePayment(
            @Parameter(description = "Authorization reference to capture (Stripe PaymentIntent id or AllSecure preauth UUID)") @PathVariable String paymentIntentId) {
        if (!isVendorOrAdmin()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("success", false, "message", "Only vendors and admins can capture payments"));
        }
        if (!callerOwnsPayment(paymentIntentId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("success", false, "message", "You can only capture payments for your own orders"));
        }
        try {
            paymentProviderRouter.active().capture(paymentIntentId);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Payment capture requested successfully",
                "paymentReference", paymentIntentId
            ));
        } catch (RuntimeException e) {
            logger.error("Failed to capture payment {}: {}", paymentIntentId, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error capturing payment {}: {}", paymentIntentId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "An unexpected error occurred"));
        }
    }

    /**
     * Cancels a PaymentIntent (releases the hold) - called when order is cancelled
     * This implements the Too Good To Go style payment flow where payment authorization
     * is released if order is cancelled before pickup
     */
    @PostMapping("/cancel/{paymentIntentId}")
    @Operation(
        summary = "Cancel payment",
        description = "Cancels a previously authorized PaymentIntent. This releases the hold on the customer's card. " +
                      "Used in Too Good To Go style flow when an order is cancelled before pickup."
    )
    public ResponseEntity<Map<String, Object>> cancelPayment(
            @Parameter(description = "Stripe PaymentIntent ID to cancel") @PathVariable String paymentIntentId) {
        if (!isVendorOrAdmin()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("success", false, "message", "Only vendors and admins can cancel payments"));
        }
        if (!callerOwnsPayment(paymentIntentId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("success", false, "message", "You can only cancel payments for your own orders"));
        }
        try {
            PaymentIntent cancelledIntent = paymentService.cancelPaymentIntent(paymentIntentId);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Payment cancelled successfully",
                "paymentIntentId", cancelledIntent.getId(),
                "status", cancelledIntent.getStatus()
            ));
        } catch (StripeException e) {
            logger.error("Failed to cancel PaymentIntent {}: {}", paymentIntentId, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", "Failed to cancel payment: " + e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error cancelling PaymentIntent {}: {}", paymentIntentId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "An unexpected error occurred"));
        }
    }

}
