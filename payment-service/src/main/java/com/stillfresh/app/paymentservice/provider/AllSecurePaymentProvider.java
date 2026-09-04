package com.stillfresh.app.paymentservice.provider;

import com.stillfresh.app.paymentservice.allsecure.AllSecureCallback;
import com.stillfresh.app.paymentservice.allsecure.AllSecureClient;
import com.stillfresh.app.paymentservice.allsecure.AllSecureProperties;
import com.stillfresh.app.paymentservice.allsecure.AllSecureResult;
import com.stillfresh.app.paymentservice.dto.CardRegistrationRequest;
import com.stillfresh.app.paymentservice.dto.CardRegistrationResult;
import com.stillfresh.app.paymentservice.dto.CustomerPaymentMethodDto;
import com.stillfresh.app.paymentservice.dto.PaymentStatusResponse;
import com.stillfresh.app.paymentservice.model.AuthorizationStatus;
import com.stillfresh.app.paymentservice.model.CustomerPaymentMethod;
import com.stillfresh.app.paymentservice.model.PaymentTransaction;
import com.stillfresh.app.paymentservice.publisher.PaymentEventPublisher;
import com.stillfresh.app.paymentservice.repository.CustomerPaymentMethodRepository;
import com.stillfresh.app.paymentservice.repository.PaymentTransactionRepository;
import com.stillfresh.app.paymentservice.service.LedgerService;
import com.stillfresh.app.sharedentities.payment.events.OrderPaymentSettledEvent;
import com.stillfresh.app.sharedentities.payment.events.PaymentCapturedEvent;
import com.stillfresh.app.sharedentities.payment.events.PaymentFailureEvent;
import com.stillfresh.app.sharedentities.payment.events.PaymentRequestEvent;
import com.stillfresh.app.sharedentities.payment.events.PaymentSuccessEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * {@link PaymentProvider} backed by the AllSecure Exchange Platform.
 *
 * <p>Card registration uses a hosted-redirect Register transaction; the card is persisted locally when
 * the async callback arrives. Order placement uses a card-on-file Preauthorize against the stored
 * reference; pickup captures it; cancellation voids it.</p>
 */
@Component
public class AllSecurePaymentProvider implements PaymentProvider {

    private static final Logger logger = LoggerFactory.getLogger(AllSecurePaymentProvider.class);

    public static final String NAME = "allsecure";
    private static final String INDICATOR_CARDONFILE = "CARDONFILE";

    @Autowired
    private AllSecureClient client;

    @Autowired
    private AllSecureProperties properties;

    @Autowired
    private CustomerPaymentMethodRepository paymentMethodRepository;

    @Autowired
    private PaymentTransactionRepository paymentTransactionRepository;

    @Autowired
    private PaymentEventPublisher eventPublisher;

    @Autowired
    private LedgerService ledgerService;

    @Autowired
    private com.stillfresh.app.paymentservice.service.PlatformSettingsService platformSettingsService;

    @Override
    public String name() {
        return NAME;
    }

    // ── Card management ───────────────────────────────────────────────────────

    @Override
    public CardRegistrationResult registerCard(CardRegistrationRequest request, Principal principal) {
        String username = principal.getName();
        String transactionId = "reg-" + UUID.randomUUID();
        logger.info("Starting AllSecure hosted card registration for username={}, transactionId={}", username, transactionId);

        AllSecureResult result = client.register(transactionId, username,
                "Card registration for " + username);

        if (result.isRedirect() && result.getRedirectUrl() != null) {
            return CardRegistrationResult.redirect(NAME, result.getRedirectUrl(), transactionId,
                    "Open the redirect URL to enter your card details.");
        }
        if (result.isFinished()) {
            // Some connectors finish registration without a redirect; persist immediately if we have a reference.
            if (result.getReferenceId() != null) {
                storeCard(username, result.getReferenceId(), result.getRegistrationId(), null, null, null, null);
            }
            return CardRegistrationResult.redirect(NAME, null, transactionId, "Card registered successfully.");
        }
        if (result.isError()) {
            throw new com.stillfresh.app.paymentservice.allsecure.AllSecureException(
                    result.getErrorMessage() != null ? result.getErrorMessage() : "Card registration failed",
                    result.getErrorCode());
        }
        // PENDING or unexpected: surface the reference; final state arrives via callback.
        return CardRegistrationResult.redirect(NAME, result.getRedirectUrl(), transactionId,
                "Card registration pending confirmation.");
    }

    @Override
    public List<CustomerPaymentMethodDto> listPaymentMethods(Principal principal) {
        String username = principal.getName();
        List<CustomerPaymentMethod> cards = paymentMethodRepository.findByUsernameOrderByCreatedAtDesc(username);
        List<CustomerPaymentMethodDto> result = new ArrayList<>();
        for (CustomerPaymentMethod card : cards) {
            result.add(toDto(card));
        }
        return result;
    }

    @Override
    public void deletePaymentMethod(String paymentMethodId, Principal principal) {
        String username = principal.getName();
        CustomerPaymentMethod card = paymentMethodRepository
                .findByUsernameAndReferenceId(username, paymentMethodId)
                .orElseThrow(() -> new RuntimeException("Payment method not found for user"));
        try {
            client.deregister("dereg-" + UUID.randomUUID(), card.getReferenceId());
        } catch (Exception e) {
            logger.warn("AllSecure deregister failed for referenceId {} (continuing to delete locally): {}",
                    card.getReferenceId(), e.getMessage());
        }
        paymentMethodRepository.delete(card);
        logger.info("Deleted AllSecure payment method referenceId={} for username={}", paymentMethodId, username);
    }

    // ── Order lifecycle ───────────────────────────────────────────────────────

    @Override
    public void preauthorize(PaymentRequestEvent event) {
        String username = event.getUsername();
        if (username == null || username.isBlank()) {
            publishFailure(event, "Username missing in payment event");
            return;
        }

        Optional<CustomerPaymentMethod> cardOpt = paymentMethodRepository.findFirstByUsernameAndIsDefaultTrue(username);
        if (cardOpt.isEmpty()) {
            cardOpt = paymentMethodRepository.findFirstByUsernameOrderByCreatedAtDesc(username);
        }
        if (cardOpt.isEmpty()) {
            publishFailure(event, "No registered card found for user: " + username);
            return;
        }
        CustomerPaymentMethod card = cardOpt.get();

        long gross = event.getAmount() != null ? event.getAmount() : 0L;
        double platformFeePercent = platformSettingsService.getFeePercent();
        long platformFee = Math.round(gross * (platformFeePercent / 100.0));
        long net = gross - platformFee;
        String currency = currencyOf(event);
        String amount = toDecimal(gross);
        String transactionId = event.getRequestId() != null ? event.getRequestId() : "preauth-" + UUID.randomUUID();

        logger.info("AllSecure preauthorize requestId={}, username={}, amount={} {}, referenceTransactionId={}",
                transactionId, username, amount, currency, card.getReferenceId());

        AllSecureResult result;
        try {
            result = client.preauthorize(transactionId, username, amount, currency,
                    "Order payment", card.getReferenceId(), INDICATOR_CARDONFILE);
        } catch (Exception e) {
            publishFailure(event, "AllSecure preauthorize failed: " + e.getMessage());
            return;
        }

        if (result.isError() || result.getReferenceId() == null) {
            publishFailure(event, result.getErrorMessage() != null ? result.getErrorMessage() : "Preauthorization failed");
            return;
        }

        PaymentTransaction tx = saveTransaction(event, result.getReferenceId(), gross, platformFee, net, currency, platformFeePercent);

        if (result.isFinished()) {
            publishSuccessOnce(tx);
        } else if (requiresAuthentication(result)) {
            tx.setRedirectUrl(result.getRedirectUrl());
            tx.setAuthorizationStatus(AuthorizationStatus.AUTHENTICATION_REQUIRED);
            paymentTransactionRepository.save(tx);
            logger.info("AllSecure preauthorize for requestId={} requires 3DS; redirectUrl exposed for client polling.",
                    transactionId);
        } else {
            tx.setAuthorizationStatus(AuthorizationStatus.PROCESSING);
            paymentTransactionRepository.save(tx);
            logger.info("AllSecure preauthorize PENDING for requestId={}, awaiting callback.", transactionId);
        }
    }

    /**
     * Poll preauthorization progress for a place-order {@code requestId}. Returns {@code PROCESSING}
     * when no row exists yet (offer validation still in flight).
     */
    /**
     * @return empty if the transaction exists but does not belong to {@code userId} (caller forbidden)
     */
    public java.util.Optional<PaymentStatusResponse> getPaymentStatus(String requestId, Long userId) {
        if (requestId == null || requestId.isBlank()) {
            throw new IllegalArgumentException("requestId is required");
        }
        Optional<PaymentTransaction> txOpt = paymentTransactionRepository.findByRequestId(requestId);
        if (txOpt.isEmpty()) {
            return java.util.Optional.of(PaymentStatusResponse.processing(requestId));
        }
        PaymentTransaction tx = txOpt.get();
        if (userId == null || !userId.equals(tx.getUserId())) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.of(toPaymentStatusResponse(tx));
    }

    @Override
    public void capture(String referenceId) {
        if (referenceId == null || referenceId.isBlank()) {
            logger.warn("AllSecure capture called with blank referenceId; skipping.");
            return;
        }
        Optional<PaymentTransaction> txOpt = paymentTransactionRepository.findByPaymentIntentId(referenceId);
        String amount = txOpt.map(t -> toDecimal(t.getGrossAmountCents())).orElse(null);
        String currency = txOpt.map(PaymentTransaction::getCurrency).orElse(properties.getCurrency());

        AllSecureResult result;
        try {
            result = client.capture("cap-" + UUID.randomUUID(), referenceId, amount, currency);
        } catch (Exception e) {
            logger.error("AllSecure capture failed for referenceId {}: {}", referenceId, e.getMessage(), e);
            return;
        }

        if (result.isError()) {
            logger.error("AllSecure capture returned error for referenceId {}: {} ({})",
                    referenceId, result.getErrorMessage(), result.getErrorCode());
            return;
        }
        if (result.isFinished()) {
            settleCapture(referenceId);
        } else {
            logger.info("AllSecure capture for referenceId {} is {}; settlement deferred to callback.",
                    referenceId, result.getReturnType());
        }
    }

    @Override
    public void cancel(String referenceId) {
        if (referenceId == null || referenceId.isBlank()) {
            logger.warn("AllSecure cancel called with blank referenceId; skipping.");
            return;
        }
        try {
            AllSecureResult result = client.voidTransaction("void-" + UUID.randomUUID(), referenceId);
            if (result.isError()) {
                logger.error("AllSecure void returned error for referenceId {}: {} ({})",
                        referenceId, result.getErrorMessage(), result.getErrorCode());
            } else {
                logger.info("AllSecure void succeeded for referenceId {} (returnType={})",
                        referenceId, result.getReturnType());
            }
        } catch (Exception e) {
            logger.error("AllSecure void failed for referenceId {}: {}", referenceId, e.getMessage(), e);
        }
    }

    // ── Callback handlers (invoked by the callback controller) ───────────────

    /** Persists a card when a Register callback arrives successfully. */
    public void onRegisterCallback(AllSecureCallback cb) {
        String username = cb.getCustomerIdentification();
        if (username == null || username.isBlank()) {
            logger.warn("AllSecure register callback missing customer identification; cannot persist card (referenceId={}).",
                    cb.getReferenceId());
            return;
        }
        if (!cb.isOk() || cb.getReferenceId() == null) {
            logger.warn("AllSecure register callback not OK for username={}: {} ({})",
                    username, cb.getErrorMessage(), cb.getErrorCode());
            return;
        }
        storeCard(username, cb.getReferenceId(), null,
                cb.getCardType(), cb.getCardLastFourDigits(), cb.getCardExpiryMonth(), cb.getCardExpiryYear());
    }

    /** Confirms or fails a preauthorization when its callback arrives. */
    public void onPreauthCallback(AllSecureCallback cb) {
        if (cb.getReferenceId() == null) {
            logger.warn("AllSecure preauth callback missing referenceId; ignoring.");
            return;
        }
        Optional<PaymentTransaction> txOpt = paymentTransactionRepository.findByPaymentIntentId(cb.getReferenceId());
        if (txOpt.isEmpty()) {
            logger.warn("AllSecure preauth callback for unknown referenceId={}; ignoring.", cb.getReferenceId());
            return;
        }
        PaymentTransaction tx = txOpt.get();
        if (cb.isOk()) {
            publishSuccessOnce(tx);
        } else {
            String reason = cb.getErrorMessage() != null ? cb.getErrorMessage() : "Preauthorization failed";
            tx.setAuthorizationStatus(AuthorizationStatus.FAILED);
            tx.setFailureReason(reason);
            tx.setRedirectUrl(null);
            paymentTransactionRepository.save(tx);
            eventPublisher.publishPaymentFailureEvent(new PaymentFailureEvent(
                    tx.getRequestId(), tx.getUserId(), tx.getOfferId(), reason));
        }
    }

    /** Settles a capture when its callback arrives. */
    public void onCaptureCallback(AllSecureCallback cb) {
        // Capture callbacks reference the original preauthorize via referenceId.
        if (cb.getReferenceId() != null && cb.isOk()) {
            settleCapture(cb.getReferenceId());
        } else {
            logger.warn("AllSecure capture callback not settled (referenceId={}, result={}).",
                    cb.getReferenceId(), cb.getResult());
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void storeCard(String username, String referenceId, String registrationId,
                           String brand, String last4, String expMonth, String expYear) {
        Optional<CustomerPaymentMethod> existing = paymentMethodRepository.findByReferenceId(referenceId);
        if (existing.isPresent()) {
            logger.info("AllSecure card with referenceId={} already stored; skipping.", referenceId);
            return;
        }
        CustomerPaymentMethod card = new CustomerPaymentMethod();
        card.setUsername(username);
        card.setReferenceId(referenceId);
        card.setRegistrationId(registrationId);
        card.setBrand(brand);
        card.setLast4(last4);
        card.setExpMonth(expMonth);
        card.setExpYear(expYear);
        // First card for the user becomes the default.
        boolean isFirst = paymentMethodRepository.findByUsernameOrderByCreatedAtDesc(username).isEmpty();
        card.setDefault(isFirst);
        paymentMethodRepository.save(card);
        logger.info("Stored AllSecure card for username={}, referenceId={}, brand={}, last4={}, default={}",
                username, referenceId, brand, last4, isFirst);
    }

    private PaymentTransaction saveTransaction(PaymentRequestEvent event, String referenceId,
                                               long gross, long platformFee, long net, String currency,
                                               double feePercentApplied) {
        return paymentTransactionRepository.findByPaymentIntentId(referenceId).orElseGet(() -> {
            PaymentTransaction tx = new PaymentTransaction();
            tx.setRequestId(event.getRequestId());
            tx.setPaymentIntentId(referenceId);
            tx.setUserId(event.getUserId());
            tx.setVendorId(event.getVendorId());
            tx.setOfferId(event.getOfferId());
            tx.setGrossAmountCents(gross);
            tx.setPlatformFeeCents(platformFee);
            tx.setNetAmountCents(net);
            tx.setFeePercentApplied(feePercentApplied);
            tx.setCurrency(currency);
            return paymentTransactionRepository.save(tx);
        });
    }

    private void publishSuccessOnce(PaymentTransaction tx) {
        if (tx.isSuccessNotified()) {
            logger.debug("PaymentSuccessEvent already published for referenceId={}; skipping.", tx.getPaymentIntentId());
            return;
        }
        PaymentSuccessEvent successEvent = new PaymentSuccessEvent(
                tx.getRequestId(), tx.getUserId(), tx.getOfferId(), tx.getPaymentIntentId());
        successEvent.setPaymentProvider(NAME);
        eventPublisher.publishPaymentSuccessEvent(successEvent);
        tx.setSuccessNotified(true);
        tx.setAuthorizationStatus(AuthorizationStatus.AUTHORIZED);
        tx.setRedirectUrl(null);
        paymentTransactionRepository.save(tx);
        logger.info("Published PaymentSuccessEvent for AllSecure referenceId={}", tx.getPaymentIntentId());
    }

    private void settleCapture(String referenceId) {
        paymentTransactionRepository.findByPaymentIntentId(referenceId).ifPresentOrElse(tx -> {
            boolean alreadySettled = tx.isLedgerWritten();
            try {
                ledgerService.writePaymentLedger(tx);
            } catch (Exception e) {
                logger.error("Failed to write ledger for AllSecure referenceId={}: {}", referenceId, e.getMessage(), e);
            }
            if (!alreadySettled) {
                eventPublisher.publishPaymentCapturedEvent(new PaymentCapturedEvent(referenceId, "succeeded"));
                eventPublisher.publishOrderPaymentSettledEvent(new OrderPaymentSettledEvent(
                        referenceId, tx.getUserId(), tx.getVendorId(), tx.getOfferId(),
                        tx.getGrossAmountCents(), tx.getPlatformFeeCents(), tx.getNetAmountCents(),
                        tx.getCurrency(), tx.getFeePercentApplied()));
                logger.info("Settled AllSecure capture for referenceId={}", referenceId);
            }
        }, () -> logger.warn("No PaymentTransaction found for AllSecure referenceId={}; capture settlement skipped.", referenceId));
    }

    private void publishFailure(PaymentRequestEvent event, String reason) {
        logger.warn("AllSecure payment failed for requestId={}: {}", event.getRequestId(), reason);
        markFailed(event, reason);
        eventPublisher.publishPaymentFailureEvent(new PaymentFailureEvent(
                event.getRequestId(), event.getUserId(), event.getOfferId(), reason));
    }

    private void markFailed(PaymentRequestEvent event, String reason) {
        if (event.getRequestId() == null) {
            return;
        }
        PaymentTransaction tx = paymentTransactionRepository.findByRequestId(event.getRequestId())
                .orElseGet(() -> {
                    PaymentTransaction created = new PaymentTransaction();
                    created.setRequestId(event.getRequestId());
                    created.setUserId(event.getUserId());
                    created.setOfferId(event.getOfferId());
                    created.setVendorId(event.getVendorId());
                    created.setGrossAmountCents(event.getAmount() != null ? event.getAmount() : 0L);
                    created.setPlatformFeeCents(0L);
                    created.setNetAmountCents(created.getGrossAmountCents());
                    created.setCurrency(currencyOf(event));
                    return created;
                });
        tx.setAuthorizationStatus(AuthorizationStatus.FAILED);
        tx.setFailureReason(reason);
        tx.setRedirectUrl(null);
        paymentTransactionRepository.save(tx);
    }

    private static boolean requiresAuthentication(AllSecureResult result) {
        return result.isRedirect()
                || (result.getRedirectUrl() != null && !result.getRedirectUrl().isBlank());
    }

    private PaymentStatusResponse toPaymentStatusResponse(PaymentTransaction tx) {
        PaymentStatusResponse response = new PaymentStatusResponse();
        response.setRequestId(tx.getRequestId());
        response.setOfferId(tx.getOfferId());
        response.setPaymentIntentId(tx.getPaymentIntentId());
        response.setFailureReason(tx.getFailureReason());

        if (tx.isSuccessNotified() || tx.getAuthorizationStatus() == AuthorizationStatus.AUTHORIZED) {
            response.setStatus(AuthorizationStatus.AUTHORIZED.name());
            response.setMessage("Payment authorized. Your order should be confirmed shortly.");
        } else if (tx.getAuthorizationStatus() == AuthorizationStatus.FAILED) {
            response.setStatus(AuthorizationStatus.FAILED.name());
            response.setMessage(tx.getFailureReason() != null ? tx.getFailureReason() : "Payment failed.");
        } else if (tx.getAuthorizationStatus() == AuthorizationStatus.AUTHENTICATION_REQUIRED
                || (tx.getRedirectUrl() != null && !tx.getRedirectUrl().isBlank())) {
            response.setStatus(AuthorizationStatus.AUTHENTICATION_REQUIRED.name());
            response.setRedirectUrl(tx.getRedirectUrl());
            response.setMessage("Complete payment authentication to confirm your order.");
        } else {
            response.setStatus(AuthorizationStatus.PROCESSING.name());
            response.setMessage("Payment is being processed.");
        }
        return response;
    }

    private String currencyOf(PaymentRequestEvent event) {
        if (event.getCurrency() != null && event.getCurrency().getIsoCode() != null) {
            return event.getCurrency().getIsoCode();
        }
        return properties.getCurrency();
    }

    /** Converts minor units (cents) to a dot-decimal string, e.g. 1234 -> "12.34". */
    private static String toDecimal(long cents) {
        return BigDecimal.valueOf(cents).movePointLeft(2).toPlainString();
    }

    private CustomerPaymentMethodDto toDto(CustomerPaymentMethod card) {
        CustomerPaymentMethodDto dto = new CustomerPaymentMethodDto();
        dto.setPaymentMethodId(card.getReferenceId());
        dto.setType("card");
        dto.setCardBrand(card.getBrand());
        dto.setCardLast4(card.getLast4());
        dto.setCardExpMonth(parseLong(card.getExpMonth()));
        dto.setCardExpYear(parseLong(card.getExpYear()));
        dto.setIsDefault(card.isDefault());
        return dto;
    }

    private static Long parseLong(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return Long.parseLong(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
