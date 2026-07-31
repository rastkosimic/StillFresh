package com.stillfresh.app.paymentservice.controller;

import com.stillfresh.app.paymentservice.service.MoRPayoutService;
import com.stillfresh.app.paymentservice.security.JwtUtil;
import com.stillfresh.app.sharedentities.responses.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/mor")
@Tag(name = "Admin MoR Payment Management", description = "Admin endpoints for managing MoR (Merchant of Record) vendor payments and payouts")
public class AdminController {

    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

    @Autowired
    private MoRPayoutService morPayoutService;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * Get all pending payouts for MoR vendors
     * Returns payouts with status PENDING or PROCESSING, including vendor and bank details
     */
    @GetMapping("/payouts/pending")
    @Operation(
        summary = "Get all pending MoR payouts",
        description = "Returns all payouts with status PENDING or PROCESSING for MoR vendors, including vendor information and bank details for processing"
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Pending payouts retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> getAllMoRPendingPayouts() {
        try {
            logger.info("Admin requesting all pending MoR payouts");
            List<Map<String, Object>> payouts = morPayoutService.getPendingPayouts();
            return ResponseEntity.ok(payouts);
        } catch (Exception e) {
            logger.error("Error retrieving pending MoR payouts: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Failed to retrieve pending payouts: " + e.getMessage()));
        }
    }

    /**
     * Get all MoR vendors with their balances
     * Shows which vendors have balances and pending payouts
     */
    @GetMapping("/vendors/balances")
    @Operation(
        summary = "Get all MoR vendors with balances",
        description = "Returns all MoR vendors with their current balance, pending payout counts, and bank details status"
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Vendor balances retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> getAllMoRVendorsWithBalances() {
        try {
            logger.info("Admin requesting all MoR vendors with balances");
            List<Map<String, Object>> vendors = morPayoutService.getVendorBalances();
            return ResponseEntity.ok(vendors);
        } catch (Exception e) {
            logger.error("Error retrieving MoR vendor balances: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Failed to retrieve vendor balances: " + e.getMessage()));
        }
    }

    /**
     * Get all order payments for MoR vendors
     * Shows which orders generated payments that need to be paid to vendors
     */
    @GetMapping("/transactions/orders")
    @Operation(
        summary = "Get all MoR order payments",
        description = "Returns all ORDER_PAYMENT transactions for MoR vendors, showing orders and amounts that need to be paid. Can be filtered by date range."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Order payments retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid date format"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> getAllMoROrderPayments(
            @Parameter(description = "Start date in ISO 8601 format (e.g., 2024-01-01T00:00:00Z)")
            @RequestParam(value = "from", required = false) String from,
            @Parameter(description = "End date in ISO 8601 format (e.g., 2024-01-31T23:59:59Z)")
            @RequestParam(value = "to", required = false) String to) {
        try {
            logger.info("Admin requesting MoR order payments. From: {}, To: {}", from, to);
            
            OffsetDateTime fromDate = null;
            OffsetDateTime toDate = null;
            
            if (from != null && !from.isEmpty()) {
                try {
                    fromDate = OffsetDateTime.parse(from);
                } catch (Exception e) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ErrorResponse("Invalid 'from' date format. Use ISO 8601 format (e.g., 2024-01-01T00:00:00Z)"));
                }
            }
            
            if (to != null && !to.isEmpty()) {
                try {
                    toDate = OffsetDateTime.parse(to);
                } catch (Exception e) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ErrorResponse("Invalid 'to' date format. Use ISO 8601 format (e.g., 2024-01-31T23:59:59Z)"));
                }
            }
            
            List<Map<String, Object>> transactions = morPayoutService.getOrderPayments(fromDate, toDate);
            return ResponseEntity.ok(transactions);
        } catch (Exception e) {
            logger.error("Error retrieving MoR order payments: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Failed to retrieve order payments: " + e.getMessage()));
        }
    }

    /**
     * Get payout summary for MoR vendors
     * Shows statistics about pending, processing, completed, and failed payouts
     */
    @GetMapping("/payouts/summary")
    @Operation(
        summary = "Get MoR payout summary",
        description = "Returns summary statistics for MoR payouts including counts and totals by status"
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Payout summary retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> getMoRPayoutSummary() {
        try {
            logger.info("Admin requesting MoR payout summary");
            Map<String, Object> summary = morPayoutService.getPayoutSummary();
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            logger.error("Error retrieving MoR payout summary: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Failed to retrieve payout summary: " + e.getMessage()));
        }
    }

    /**
     * Update payout status
     * Allows admin to mark payouts as PROCESSING, COMPLETED, or FAILED
     */
    @PutMapping("/payouts/{payoutId}/status")
    @Operation(
        summary = "Update payout status",
        description = "Updates the status of a payout (PROCESSING, COMPLETED, or FAILED). Can also add transaction reference and notes."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Payout status updated successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid status or payout not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> updatePayoutStatus(
            @Parameter(description = "Payout ID")
            @PathVariable Long payoutId,
            @Parameter(description = "New status: PROCESSING, COMPLETED, or FAILED")
            @RequestParam String status,
            @Parameter(description = "Optional transaction reference (e.g., bank transfer reference)")
            @RequestParam(value = "transactionReference", required = false) String transactionReference,
            @Parameter(description = "Optional admin notes")
            @RequestParam(value = "notes", required = false) String notes) {
        try {
            logger.info("Admin updating payout {} status to {}", payoutId, status);
            morPayoutService.updatePayoutStatus(payoutId, status, transactionReference, notes);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Payout status updated successfully"
            ));
        } catch (RuntimeException e) {
            logger.error("Error updating payout status: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error updating payout status: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Failed to update payout status: " + e.getMessage()));
        }
    }

    /**
     * Get all payouts for the authenticated vendor (from JWT)
     * Useful for viewing a vendor's complete payout history
     */
    @GetMapping("/vendors/payouts")
    @Operation(
        summary = "Get payouts for the authenticated MoR vendor",
        description = "Returns all payouts for the authenticated MoR vendor (vendor ID extracted from JWT), including all statuses"
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Vendor payouts retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Vendor not found or not using MoR model"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> getVendorPayouts(HttpServletRequest request) {
        try {
            // Extract vendor ID from JWT token
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Missing or invalid Authorization header"));
            }
            
            String jwt = authHeader.substring(7);
            Long vendorId = jwtUtil.extractVendorId(jwt);
            if (vendorId == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("Vendor ID not found in JWT token"));
            }
            
            logger.info("Admin requesting payouts for vendor {} (from JWT)", vendorId);
            List<Map<String, Object>> payouts = morPayoutService.getVendorPayouts(vendorId);
            return ResponseEntity.ok(payouts);
        } catch (RuntimeException e) {
            logger.error("Error retrieving vendor payouts: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error retrieving vendor payouts: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Failed to retrieve vendor payouts: " + e.getMessage()));
        }
    }

}

