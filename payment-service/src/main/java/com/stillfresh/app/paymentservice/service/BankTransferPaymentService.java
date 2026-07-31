package com.stillfresh.app.paymentservice.service;

import com.stillfresh.app.paymentservice.model.BankTransferPayment;
import com.stillfresh.app.paymentservice.model.BankTransferStatus;
import com.stillfresh.app.paymentservice.publisher.PaymentEventPublisher;
import com.stillfresh.app.paymentservice.repository.BankTransferPaymentRepository;
import com.stillfresh.app.sharedentities.order.events.BankTransferOrderEvent;
import com.stillfresh.app.sharedentities.payment.events.BankTransferConfirmedEvent;
import com.stillfresh.app.sharedentities.payment.events.BankTransferInitiatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;

@Service
public class BankTransferPaymentService {

    private static final Logger logger = LoggerFactory.getLogger(BankTransferPaymentService.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Autowired
    private BankTransferPaymentRepository repository;

    @Autowired
    private LedgerService ledgerService;

    @Autowired
    private PaymentEventPublisher eventPublisher;

    // ── Platform bank account configuration ─────────────────────────────────

    @Value("${bank-transfer.platform.iban}")
    private String platformIban;

    @Value("${bank-transfer.platform.bank-name}")
    private String platformBankName;

    @Value("${bank-transfer.platform.account-holder}")
    private String platformAccountHolder;

    @Value("${bank-transfer.payment-expiry-hours:24}")
    private int expiryHours;

    @Autowired
    private PlatformSettingsService platformSettingsService;

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Creates a BankTransferPayment from a BankTransferOrderEvent and publishes
     * BankTransferInitiatedEvent so the customer receives payment instructions.
     */
    @Transactional
    public BankTransferPayment initiate(BankTransferOrderEvent event) {
        // Check for idempotency — don't double-create
        Optional<BankTransferPayment> existing = repository.findByOrderId(event.getOrderId());
        if (existing.isPresent()) {
            logger.warn("BankTransferPayment already exists for orderId={}. Returning existing.", event.getOrderId());
            return existing.get();
        }

        long grossCents = event.getGrossAmountCents();
        double feePercent = platformSettingsService.getFeePercent();
        long feeCents = calculateFee(grossCents, feePercent);
        long netCents = grossCents - feeCents;

        String reference = generateReference();
        Instant expiresAt = Instant.now().plusSeconds(expiryHours * 3600L);

        BankTransferPayment payment = new BankTransferPayment();
        payment.setPaymentReference(reference);
        payment.setOrderId(event.getOrderId());
        payment.setUserId(event.getUserId());
        payment.setVendorId(event.getVendorId());
        payment.setOfferId(event.getOfferId());
        payment.setGrossAmountCents(grossCents);
        payment.setPlatformFeeCents(feeCents);
        payment.setNetAmountCents(netCents);
        payment.setFeePercentApplied(feePercent);
        payment.setCurrency(event.getCurrency());
        payment.setIban(platformIban);
        payment.setBankName(platformBankName);
        payment.setAccountHolder(platformAccountHolder);
        payment.setExpiresAt(expiresAt);
        payment = repository.save(payment);

        logger.info("BankTransferPayment created: reference={}, orderId={}, amount={} {}",
                    reference, event.getOrderId(), grossCents, event.getCurrency());

        String description = "StillFresh order " + reference;
        eventPublisher.publishBankTransferInitiatedEvent(new BankTransferInitiatedEvent(
            event.getOrderId(), event.getUserId(), reference,
            platformIban, platformBankName, platformAccountHolder,
            grossCents, event.getCurrency(), description, expiresAt
        ));

        return payment;
    }

    /**
     * Admin confirms that the bank transfer has been received.
     * Writes ledger entries and fires BankTransferConfirmedEvent.
     */
    @Transactional
    public BankTransferPayment confirm(String paymentReference, String confirmedBy) {
        BankTransferPayment payment = repository.findByPaymentReference(paymentReference)
                .orElseThrow(() -> new IllegalArgumentException("Bank transfer not found: " + paymentReference));

        if (payment.getStatus() == BankTransferStatus.CONFIRMED) {
            logger.warn("BankTransferPayment {} is already confirmed. Skipping.", paymentReference);
            return payment;
        }
        if (payment.getStatus() == BankTransferStatus.CANCELLED) {
            throw new IllegalStateException("Cannot confirm a cancelled bank transfer: " + paymentReference);
        }

        payment.setStatus(BankTransferStatus.CONFIRMED);
        payment.setConfirmedAt(Instant.now());
        payment.setConfirmedBy(confirmedBy);
        payment = repository.save(payment);

        // Write ledger entries (same structure as Stripe capture)
        writeLedger(payment);

        // Notify downstream services
        eventPublisher.publishBankTransferConfirmedEvent(new BankTransferConfirmedEvent(
            payment.getOrderId(), payment.getUserId(), payment.getVendorId(),
            payment.getPaymentReference(),
            payment.getGrossAmountCents(), payment.getPlatformFeeCents(), payment.getNetAmountCents(),
            payment.getCurrency(), confirmedBy, payment.getFeePercentApplied()
        ));

        logger.info("BankTransferPayment {} confirmed by {}", paymentReference, confirmedBy);
        return payment;
    }

    /**
     * Marks a bank transfer as cancelled. No money movement; idempotent.
     */
    @Transactional
    public BankTransferPayment cancel(String paymentReference) {
        BankTransferPayment payment = repository.findByPaymentReference(paymentReference)
                .orElseThrow(() -> new IllegalArgumentException("Bank transfer not found: " + paymentReference));

        if (payment.getStatus() == BankTransferStatus.CONFIRMED) {
            logger.warn("Cannot cancel an already confirmed bank transfer: {}. Manual refund may be needed.", paymentReference);
            throw new IllegalStateException("Transfer already confirmed. Cannot cancel automatically.");
        }
        if (payment.getStatus() == BankTransferStatus.CANCELLED) {
            return payment; // idempotent
        }

        payment.setStatus(BankTransferStatus.CANCELLED);
        payment = repository.save(payment);
        logger.info("BankTransferPayment {} cancelled.", paymentReference);
        return payment;
    }

    /**
     * Cancels a bank transfer by orderId. Used when an order cancellation event
     * arrives for a bank-transfer order that has no paymentIntentId.
     */
    @Transactional
    public void cancelByOrderId(Long orderId) {
        repository.findByOrderId(orderId).ifPresentOrElse(
            payment -> {
                if (payment.getStatus() == BankTransferStatus.PENDING_TRANSFER
                        || payment.getStatus() == BankTransferStatus.RECEIVED) {
                    payment.setStatus(BankTransferStatus.CANCELLED);
                    repository.save(payment);
                    logger.info("BankTransferPayment for orderId={} cancelled.", orderId);
                } else {
                    logger.warn("Cannot cancel bank transfer for orderId={}: status={}", orderId, payment.getStatus());
                }
            },
            () -> logger.debug("No BankTransferPayment found for orderId={}. Nothing to cancel.", orderId)
        );
    }

    public Optional<BankTransferPayment> findByReference(String reference) {
        return repository.findByPaymentReference(reference);
    }

    public Optional<BankTransferPayment> findByOrderId(Long orderId) {
        return repository.findByOrderId(orderId);
    }

    public Page<BankTransferPayment> findPending(int page, int size) {
        return repository.findByStatusOrderByCreatedAtDesc(
                BankTransferStatus.PENDING_TRANSFER, PageRequest.of(page, size));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Generates a reference like "SF-20260314-A1B2C3". */
    private String generateReference() {
        String datePart = LocalDate.now(ZoneOffset.UTC).format(DATE_FMT);
        String randomPart = UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
        return "SF-" + datePart + "-" + randomPart;
    }

    private long calculateFee(long grossCents, double feePercent) {
        return Math.round(grossCents * feePercent / 100.0);
    }

    private void writeLedger(BankTransferPayment payment) {
        ledgerService.writeBankTransferLedger(
            payment.getVendorId(), payment.getOfferId(), payment.getUserId(),
            payment.getPaymentReference(),
            payment.getGrossAmountCents(), payment.getPlatformFeeCents(),
            payment.getNetAmountCents(), payment.getCurrency()
        );
    }
}
