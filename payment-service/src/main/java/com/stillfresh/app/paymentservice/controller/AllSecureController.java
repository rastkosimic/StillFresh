package com.stillfresh.app.paymentservice.controller;

import com.stillfresh.app.paymentservice.allsecure.AllSecureCallback;
import com.stillfresh.app.paymentservice.allsecure.AllSecureException;
import com.stillfresh.app.paymentservice.allsecure.AllSecureProperties;
import com.stillfresh.app.paymentservice.allsecure.AllSecureSignatureService;
import com.stillfresh.app.paymentservice.allsecure.AllSecureXml;
import com.stillfresh.app.paymentservice.dto.CardRegistrationRequest;
import com.stillfresh.app.paymentservice.dto.CardRegistrationResult;
import com.stillfresh.app.paymentservice.dto.CustomerPaymentMethodDto;
import com.stillfresh.app.paymentservice.dto.PaymentStatusResponse;
import com.stillfresh.app.paymentservice.provider.AllSecurePaymentProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

/**
 * AllSecure-specific endpoints (hosted card registration, stored-card management, browser landing,
 * and the asynchronous gateway callback). These live alongside the Stripe endpoints in
 * {@link PaymentController}; the active provider for the order flow is selected via {@code payment.provider}.
 */
@RestController
@RequestMapping("/payment/allsecure")
@Tag(name = "AllSecure", description = "AllSecure Exchange payment endpoints")
public class AllSecureController {

    private static final Logger logger = LoggerFactory.getLogger(AllSecureController.class);

    @Autowired
    private AllSecurePaymentProvider allSecureProvider;

    @Autowired
    private AllSecureSignatureService signatureService;

    @Autowired
    private AllSecureProperties properties;

    /**
     * Starts a hosted card registration. Returns a redirect URL the client must open so the customer
     * can enter their card (and complete 3DS) on AllSecure's hosted page. The card is persisted
     * asynchronously when the gateway calls {@link #callback}.
     */
    @PostMapping("/register-card")
    @Operation(summary = "Register a card (AllSecure hosted flow)",
            description = "Returns a redirectUrl for the client to open; the card is stored via the async callback.")
    public ResponseEntity<?> registerCard(@RequestBody(required = false) CardRegistrationRequest request,
                                          Principal principal) {
        try {
            CardRegistrationResult result = allSecureProvider.registerCard(
                    request != null ? request : new CardRegistrationRequest(), principal);
            return ResponseEntity.ok(Map.of(
                    "provider", result.getProvider(),
                    "redirectUrl", result.getRedirectUrl() != null ? result.getRedirectUrl() : "",
                    "transactionId", result.getTransactionId() != null ? result.getTransactionId() : "",
                    "message", result.getMessage() != null ? result.getMessage() : ""));
        } catch (AllSecureException e) {
            logger.warn("AllSecure card registration failed: {} ({})", e.getMessage(), e.getErrorCode());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage(), "errorCode", e.getErrorCode() != null ? e.getErrorCode() : "ALLSECURE_ERROR"));
        } catch (Exception e) {
            logger.error("Unexpected error during AllSecure card registration: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An unexpected error occurred. Please try again."));
        }
    }

    @GetMapping("/payment-methods")
    @Operation(summary = "List stored AllSecure cards", description = "Lists the authenticated customer's stored cards.")
    public ResponseEntity<List<CustomerPaymentMethodDto>> listPaymentMethods(Principal principal) {
        return ResponseEntity.ok(allSecureProvider.listPaymentMethods(principal));
    }

    /**
     * Poll preauthorization state after {@code POST /orders/place-order}. When status is
     * {@code AUTHENTICATION_REQUIRED}, open {@code redirectUrl} in a WebView so the customer can
     * complete 3DS; then poll until {@code AUTHORIZED} or {@code FAILED}.
     */
    @GetMapping("/payment-status/{requestId}")
    @Operation(summary = "Poll AllSecure preauth status for a place-order requestId")
    public ResponseEntity<?> getPaymentStatus(HttpServletRequest httpRequest,
                                              @PathVariable String requestId) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Authentication required"));
        }
        java.util.Optional<PaymentStatusResponse> status = allSecureProvider.getPaymentStatus(requestId, userId);
        if (status.isEmpty()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Payment status not available for this user"));
        }
        return ResponseEntity.ok(status.get());
    }

    @DeleteMapping("/payment-methods/{referenceId}")
    @Operation(summary = "Delete a stored AllSecure card")
    public ResponseEntity<Map<String, Object>> deletePaymentMethod(@PathVariable String referenceId,
                                                                    Principal principal) {
        try {
            allSecureProvider.deletePaymentMethod(referenceId, principal);
            return ResponseEntity.ok(Map.of("success", true, "message", "Payment method deleted"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * Browser landing endpoint AllSecure redirects to after a hosted card entry. The actual card
     * persistence happens via the server-to-server {@link #callback}. This is informational only.
     */
    @GetMapping(value = "/return", produces = MediaType.TEXT_HTML_VALUE)
    @Operation(summary = "Hosted-flow browser landing page")
    public ResponseEntity<String> returnLanding(@RequestParam(value = "status", required = false) String status) {
        String safeStatus = "success".equals(status) || "error".equals(status) || "cancel".equals(status)
                ? status : "unknown";
        String html = "<!doctype html><html><head><meta charset=\"utf-8\"><title>Payment</title></head>"
                + "<body><h3>Payment step " + safeStatus + "</h3>"
                + "<p>You can close this window and return to the app.</p></body></html>";
        return ResponseEntity.ok(html);
    }

    /**
     * Asynchronous postback from AllSecure once a transaction reaches a final state. The signature is
     * verified with the shared secret; we then persist/settle and reply HTTP 200 with body "OK".
     */
    @PostMapping(value = "/callback")
    @Operation(summary = "AllSecure status callback (server-to-server)")
    public ResponseEntity<String> callback(@RequestBody String body, HttpServletRequest httpRequest) {
        String authHeader = httpRequest.getHeader(HttpHeaders.AUTHORIZATION);
        String dateHeader = httpRequest.getHeader(HttpHeaders.DATE);
        String contentType = httpRequest.getHeader(HttpHeaders.CONTENT_TYPE);
        String requestUri = httpRequest.getRequestURI();

        String providedSignature = extractSignature(authHeader);
        boolean verified = signatureService.verify("POST", body, contentType, dateHeader, requestUri,
                properties.getSharedSecret(), providedSignature);
        if (!verified) {
            logger.warn("Rejecting AllSecure callback with invalid signature from {}", httpRequest.getRemoteAddr());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("INVALID SIGNATURE");
        }

        AllSecureCallback cb;
        try {
            cb = AllSecureXml.parseCallback(body);
        } catch (Exception e) {
            logger.error("Failed to parse AllSecure callback body: {}", e.getMessage(), e);
            // Acknowledge to avoid endless retries on a malformed-but-authentic payload.
            return ResponseEntity.ok("OK");
        }

        logger.info("Received AllSecure callback: {}", cb);
        try {
            String type = cb.getTransactionType() != null ? cb.getTransactionType().toUpperCase() : "";
            switch (type) {
                case "REGISTER" -> allSecureProvider.onRegisterCallback(cb);
                case "PREAUTHORIZE", "DEBIT" -> allSecureProvider.onPreauthCallback(cb);
                case "CAPTURE" -> allSecureProvider.onCaptureCallback(cb);
                case "VOID" -> logger.info("AllSecure void callback for referenceId={} ({})", cb.getReferenceId(), cb.getResult());
                default -> logger.info("Unhandled AllSecure callback transactionType '{}' for referenceId={}", type, cb.getReferenceId());
            }
        } catch (Exception e) {
            logger.error("Error processing AllSecure callback (referenceId={}): {}", cb.getReferenceId(), e.getMessage(), e);
        }
        return ResponseEntity.ok("OK");
    }

    /** Extracts the signature from an "Authorization: Gateway &lt;apiKey&gt;:&lt;signature&gt;" header. */
    private static String extractSignature(String authHeader) {
        if (authHeader == null) return null;
        String value = authHeader.startsWith("Gateway ") ? authHeader.substring("Gateway ".length()) : authHeader;
        int colon = value.indexOf(':');
        return colon >= 0 && colon < value.length() - 1 ? value.substring(colon + 1) : null;
    }
}
