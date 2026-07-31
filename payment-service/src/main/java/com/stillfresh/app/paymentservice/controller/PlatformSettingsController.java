package com.stillfresh.app.paymentservice.controller;

import com.stillfresh.app.paymentservice.service.PlatformSettingsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Admin endpoints for runtime-customizable platform settings. Currently the global platform fee
 * percentage used to split each captured payment into vendor credit and platform fee income.
 */
@RestController
@RequestMapping("/admin/platform")
@Tag(name = "Admin Platform Settings", description = "Admin endpoints for runtime-customizable platform settings (e.g. platform fee)")
@SecurityRequirement(name = "bearerAuth")
public class PlatformSettingsController {

    private static final Logger logger = LoggerFactory.getLogger(PlatformSettingsController.class);

    @Autowired
    private PlatformSettingsService platformSettingsService;

    @GetMapping("/fee")
    @Operation(summary = "Get platform fee", description = "Returns the current global platform fee percentage (admin only).")
    public ResponseEntity<Map<String, Object>> getFee() {
        if (!isAdmin()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("success", false, "message", "Only admins can view platform settings"));
        }
        return ResponseEntity.ok(Map.of("feePercent", platformSettingsService.getFeePercent()));
    }

    @PutMapping("/fee")
    @Operation(summary = "Update platform fee", description = "Updates the global platform fee percentage [0-100] (admin only).")
    public ResponseEntity<Map<String, Object>> updateFee(@RequestBody FeeUpdateRequest request) {
        if (!isAdmin()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("success", false, "message", "Only admins can update platform settings"));
        }
        if (request == null || request.getFeePercent() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", "feePercent is required"));
        }
        try {
            double updated = platformSettingsService.setFeePercent(request.getFeePercent());
            logger.info("Admin updated platform fee to {}%", updated);
            return ResponseEntity.ok(Map.of("success", true, "feePercent", updated));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    private static boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(r -> r.equals("ROLE_ADMIN") || r.equals("ROLE_SUPER_ADMIN"));
    }

    public static class FeeUpdateRequest {
        private Double feePercent;

        public Double getFeePercent() { return feePercent; }
        public void setFeePercent(Double feePercent) { this.feePercent = feePercent; }
    }
}
