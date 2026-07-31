package com.stillfresh.app.paymentservice.controller;

import com.stillfresh.app.paymentservice.model.BankTransferPayment;
import com.stillfresh.app.paymentservice.service.BankTransferPaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/payment/bank-transfer")
public class BankTransferController {

    private static final Logger logger = LoggerFactory.getLogger(BankTransferController.class);

    @Autowired
    private BankTransferPaymentService bankTransferPaymentService;

    /**
     * Returns payment instructions for a given reference.
     * The authenticated user must own the payment, or be an admin.
     */
    @GetMapping("/{reference}")
    public ResponseEntity<?> getByReference(@PathVariable String reference, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        Optional<BankTransferPayment> opt = bankTransferPaymentService.findByReference(reference);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        BankTransferPayment payment = opt.get();
        if (!isAdmin() && !payment.getUserId().equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(payment);
    }

    /**
     * Returns the bank transfer for a given orderId.
     * The authenticated user must own the order, or be an admin.
     */
    @GetMapping("/order/{orderId}")
    public ResponseEntity<?> getByOrderId(@PathVariable Long orderId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        Optional<BankTransferPayment> opt = bankTransferPaymentService.findByOrderId(orderId);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        BankTransferPayment payment = opt.get();
        if (!isAdmin() && !payment.getUserId().equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(payment);
    }

    /**
     * Admin: list all pending bank transfers.
     */
    @GetMapping("/pending")
    public ResponseEntity<?> listPending(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (!isAdmin()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        Page<BankTransferPayment> pending = bankTransferPaymentService.findPending(page, size);
        return ResponseEntity.ok(pending);
    }

    /**
     * Admin: confirm that a bank transfer has been received.
     * Writes ledger entries and credits the vendor.
     */
    @PostMapping("/confirm/{reference}")
    public ResponseEntity<?> confirm(@PathVariable String reference, HttpServletRequest request) {
        if (!isAdmin()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        String adminName = (String) request.getAttribute("username");
        if (adminName == null) adminName = "admin";
        try {
            BankTransferPayment payment = bankTransferPaymentService.confirm(reference, adminName);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Bank transfer confirmed. Vendor credited.",
                "reference", payment.getPaymentReference(),
                "orderId", payment.getOrderId()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "message", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error confirming bank transfer {}: {}", reference, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Unexpected error"));
        }
    }

    /**
     * Admin: cancel a pending bank transfer (order was cancelled before payment arrived).
     */
    @PostMapping("/cancel/{reference}")
    public ResponseEntity<?> cancel(@PathVariable String reference) {
        if (!isAdmin()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        try {
            BankTransferPayment payment = bankTransferPaymentService.cancel(reference);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Bank transfer cancelled.",
                "reference", payment.getPaymentReference()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "message", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    private boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(r -> r.equals("ROLE_ADMIN") || r.equals("ROLE_SUPER_ADMIN"));
    }
}
