package com.stillfresh.app.paymentservice.controller;

import com.stillfresh.app.paymentservice.model.LedgerEntry;
import com.stillfresh.app.paymentservice.model.PayoutBatch;
import com.stillfresh.app.paymentservice.model.VendorPayoutItem;
import com.stillfresh.app.paymentservice.repository.PayoutBatchRepository;
import com.stillfresh.app.paymentservice.repository.VendorPayoutItemRepository;
import com.stillfresh.app.paymentservice.service.CamtReconciliationService;
import com.stillfresh.app.paymentservice.service.LedgerService;
import com.stillfresh.app.paymentservice.service.PayoutAutoOrchestrator;
import com.stillfresh.app.paymentservice.service.PayoutAutoControlService;
import com.stillfresh.app.paymentservice.service.PayoutExecutionService;
import com.stillfresh.app.paymentservice.service.PayoutSchedulerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/ledger")
public class LedgerController {

    private static final Logger logger = LoggerFactory.getLogger(LedgerController.class);

    @Autowired
    private LedgerService ledgerService;

    @Autowired
    private PayoutSchedulerService payoutSchedulerService;

    @Autowired
    private PayoutExecutionService payoutExecutionService;

    @Autowired
    private PayoutAutoControlService payoutAutoControlService;

    @Autowired
    private PayoutAutoOrchestrator payoutAutoOrchestrator;

    @Autowired
    private CamtReconciliationService camtReconciliationService;

    @Autowired
    private PayoutBatchRepository payoutBatchRepository;

    @Autowired
    private VendorPayoutItemRepository vendorPayoutItemRepository;

    // ── Vendor-accessible endpoints ──────────────────────────────────────────

    /**
     * Returns the paginated ledger history for the authenticated vendor,
     * or any vendor if the caller is an admin.
     */
    @GetMapping("/vendor/{vendorId}")
    public ResponseEntity<?> getVendorLedger(
            @PathVariable Long vendorId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        if (!isAdminOrSelf(vendorId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Page<LedgerEntry> entries = ledgerService.getVendorLedger(vendorId, page, size);
        return ResponseEntity.ok(entries);
    }

    /**
     * Returns the current unsettled balance for a vendor, in minor currency units.
     */
    @GetMapping("/balance/{vendorId}")
    public ResponseEntity<?> getVendorBalance(@PathVariable Long vendorId) {
        if (!isAdminOrSelf(vendorId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        long balance = ledgerService.getUnsettledBalance(vendorId);
        return ResponseEntity.ok(Map.of("vendorId", vendorId, "unsettledBalanceCents", balance));
    }

    /**
     * Returns paginated payout history for the authenticated vendor.
     */
    @GetMapping("/payouts/vendor/{vendorId}")
    public ResponseEntity<?> getVendorPayoutHistory(
            @PathVariable Long vendorId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        if (!isAdminOrSelf(vendorId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        Page<VendorPayoutItem> items = vendorPayoutItemRepository
                .findByVendorIdOrderByCreatedAtDesc(vendorId, PageRequest.of(page, size));
        return ResponseEntity.ok(items);
    }

    // ── Admin-only endpoints ─────────────────────────────────────────────────

    /**
     * Returns paginated list of all payout batches (admin only).
     */
    @GetMapping("/payouts")
    public ResponseEntity<?> listPayoutBatches(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        if (!isAdmin()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        Page<PayoutBatch> batches = payoutBatchRepository
                .findAllByOrderByScheduledAtDesc(PageRequest.of(page, size));
        return ResponseEntity.ok(batches);
    }

    /**
     * Returns a single payout batch with its vendor items (admin only).
     */
    @GetMapping("/payouts/{batchId}")
    public ResponseEntity<?> getPayoutBatch(@PathVariable Long batchId) {
        if (!isAdmin()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        Optional<PayoutBatch> batch = payoutBatchRepository.findById(batchId);
        if (batch.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        List<VendorPayoutItem> items = vendorPayoutItemRepository.findByBatchId(batchId);
        return ResponseEntity.ok(Map.of("batch", batch.get(), "items", items));
    }

    /**
     * Manually triggers the payout scheduler (admin only, for testing/recovery).
     */
    @PostMapping("/payouts/run")
    public ResponseEntity<?> triggerPayoutJob() {
        if (!isAdmin()) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        logger.info("Manual payout job trigger by admin");
        payoutSchedulerService.runDailyPayoutJob();
        return ResponseEntity.ok(Map.of("message", "Payout job triggered successfully"));
    }

    /**
     * Returns a preview of every transfer that would be executed for a batch.
     * No state changes. Safe to call at any time.
     */
    @GetMapping("/payouts/{batchId}/dry-run")
    public ResponseEntity<?> dryRunBatch(@PathVariable Long batchId) {
        if (!isAdmin()) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        try {
            List<Map<String, Object>> preview = payoutExecutionService.dryRunBatch(batchId);
            return ResponseEntity.ok(Map.of("batchId", batchId, "transfers", preview));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Approves a PENDING batch for execution. Records the approver's identity.
     * No money moves at this step.
     */
    @PostMapping("/payouts/{batchId}/approve")
    public ResponseEntity<?> approveBatch(@PathVariable Long batchId) {
        if (!isAdmin()) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        try {
            String approvedBy = getCallerIdentity();
            PayoutBatch batch = payoutExecutionService.approveBatch(batchId, approvedBy);
            return ResponseEntity.ok(batch);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Executes all SCHEDULED items in an APPROVED batch.
     * Idempotent: already-COMPLETED items are skipped if called again after a crash.
     */
    @PostMapping("/payouts/{batchId}/execute")
    public ResponseEntity<?> executeBatch(@PathVariable Long batchId) {
        if (!isAdmin()) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        try {
            PayoutBatch batch = payoutExecutionService.executeBatch(batchId);
            return ResponseEntity.ok(batch);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Retries all FAILED items in a PARTIALLY_COMPLETED or FAILED batch.
     */
    @PostMapping("/payouts/{batchId}/retry-failed")
    public ResponseEntity<?> retryFailed(@PathVariable Long batchId) {
        if (!isAdmin()) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        try {
            PayoutBatch batch = payoutExecutionService.retryFailed(batchId);
            return ResponseEntity.ok(batch);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Returns all items in a batch, optionally filtered by status.
     */
    @GetMapping("/payouts/{batchId}/items")
    public ResponseEntity<?> getBatchItems(
            @PathVariable Long batchId,
            @RequestParam(required = false) String status) {
        if (!isAdmin()) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        if (!payoutBatchRepository.existsById(batchId)) return ResponseEntity.notFound().build();

        List<VendorPayoutItem> items;
        if (status != null) {
            try {
                com.stillfresh.app.sharedentities.enums.PayoutStatus ps =
                    com.stillfresh.app.sharedentities.enums.PayoutStatus.valueOf(status.toUpperCase());
                items = vendorPayoutItemRepository.findByBatchIdAndStatus(batchId, ps);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(Map.of("error", "Unknown status: " + status));
            }
        } else {
            items = vendorPayoutItemRepository.findByBatchId(batchId);
        }
        return ResponseEntity.ok(items);
    }

    @PostMapping("/payouts/auto/pause")
    public ResponseEntity<?> pauseAutoPayouts() {
        if (!isAdmin()) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        payoutAutoControlService.setAutoEnabled(false);
        return ResponseEntity.ok(Map.of("autoEnabled", false, "message", "Automatic payout pipeline paused"));
    }

    @PostMapping("/payouts/auto/resume")
    public ResponseEntity<?> resumeAutoPayouts() {
        if (!isAdmin()) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        payoutAutoControlService.setAutoEnabled(true);
        return ResponseEntity.ok(Map.of("autoEnabled", true, "message", "Automatic payout pipeline resumed"));
    }

    @GetMapping("/payouts/auto/status")
    public ResponseEntity<?> autoPayoutStatus() {
        if (!isAdmin()) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        return ResponseEntity.ok(Map.of("autoEnabled", payoutAutoControlService.isAutoEnabled()));
    }

    @PostMapping("/payouts/{batchId}/hold")
    public ResponseEntity<?> holdBatch(@PathVariable Long batchId) {
        if (!isAdmin()) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        try {
            return ResponseEntity.ok(payoutExecutionService.holdBatch(batchId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/payouts/{batchId}/release")
    public ResponseEntity<?> releaseBatch(@PathVariable Long batchId) {
        if (!isAdmin()) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        try {
            PayoutBatch batch = payoutExecutionService.releaseBatch(batchId);
            payoutAutoOrchestrator.processNewBatch(batchId);
            return ResponseEntity.ok(batch);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/payouts/{batchId}/cancel")
    public ResponseEntity<?> cancelBatch(@PathVariable Long batchId) {
        if (!isAdmin()) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        try {
            return ResponseEntity.ok(payoutExecutionService.cancelBatch(batchId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/payouts/{batchId}/items/{itemId}/hold")
    public ResponseEntity<?> holdItem(@PathVariable Long batchId, @PathVariable Long itemId) {
        if (!isAdmin()) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        try {
            return ResponseEntity.ok(payoutExecutionService.holdItem(batchId, itemId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/payouts/reconciliation/report")
    public ResponseEntity<?> reconciliationReport(
            @RequestParam(defaultValue = "7") int lookbackDays) {
        if (!isAdmin()) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        return ResponseEntity.ok(camtReconciliationService.buildReconciliationReport(lookbackDays));
    }

    /**
     * Returns the most recently completed payout for a vendor, or 204 if none exists.
     * Used by the vendor dashboard to show "last payout" info.
     */
    @GetMapping("/vendors/{vendorId}/last-payout")
    public ResponseEntity<?> getLastPayout(@PathVariable Long vendorId) {
        if (!isAdminOrSelf(vendorId)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        return vendorPayoutItemRepository
                .findTopByVendorIdAndStatusOrderByProcessedAtDesc(vendorId, com.stillfresh.app.sharedentities.enums.PayoutStatus.COMPLETED)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private String getCallerIdentity() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return "unknown";
        return auth.getName() != null ? auth.getName() : "unknown";
    }

    private boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(r -> r.equals("ROLE_ADMIN") || r.equals("ROLE_SUPER_ADMIN"));
    }

    private boolean isAdminOrSelf(Long requestedVendorId) {
        if (isAdmin()) return true;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        // Check if caller is a vendor whose ID matches
        boolean isVendorRole = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(r -> r.equals("ROLE_VENDOR") || r.equals("ROLE_VENDOR_ADMIN"));
        if (!isVendorRole) return false;
        try {
            Object principal = auth.getPrincipal();
            if (principal instanceof org.springframework.security.core.userdetails.UserDetails ud) {
                // The vendor ID is stored as a custom attribute injected by GatewayTrustFilter
                // We match by checking the name attribute if it holds the vendorId as string
                String name = ud.getUsername();
                return name != null && name.equals(String.valueOf(requestedVendorId));
            }
        } catch (Exception ignored) {}
        return false;
    }
}
