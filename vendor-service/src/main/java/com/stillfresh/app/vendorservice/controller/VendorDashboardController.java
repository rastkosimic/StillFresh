package com.stillfresh.app.vendorservice.controller;

import com.stillfresh.app.vendorservice.dto.VendorDashboardResponse;
import com.stillfresh.app.vendorservice.service.VendorDashboardService;
import com.stillfresh.app.vendorservice.security.CustomVendorDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;
import java.util.Map;

/**
 * Vendor analytics dashboard — single endpoint that aggregates order stats,
 * offer performance, ratings, and payout balance into one response.
 *
 * GET /vendors/{vendorId}/dashboard?period=today|week|month|all&offerIds=101&offerIds=102
 */
@RestController
@RequestMapping("/vendors")
public class VendorDashboardController {

    @Autowired
    private VendorDashboardService dashboardService;

    /**
     * Returns the aggregated dashboard for a vendor.
     *
     * @param vendorId  the vendor whose dashboard to load
     * @param period    "today", "week" (default), "month", or "all"
     * @param offerIds    optional repeatable filter; omit for all offers
     */
    @GetMapping("/{vendorId}/dashboard")
    public ResponseEntity<?> getDashboard(
            @PathVariable Long vendorId,
            @RequestParam(value = "period", defaultValue = "week") String period,
            @RequestParam(value = "offerIds", required = false) List<Long> offerIds,
            HttpServletRequest request) {

        if (!isAdminOrSelf(vendorId, request)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            VendorDashboardResponse dashboard = dashboardService.buildDashboard(vendorId, period, offerIds);
            return ResponseEntity.ok(dashboard);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    // ── Auth helpers ──────────────────────────────────────────────────────────

    private boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(r -> r.equals("ROLE_ADMIN") || r.equals("ROLE_SUPER_ADMIN"));
    }

    private boolean isAdminOrSelf(Long requestedVendorId, HttpServletRequest request) {
        if (isAdmin()) return true;

        // Prefer the gateway-provided numeric user id (set by GatewayTrustFilter)
        Object reqUserId = request != null ? request.getAttribute("userId") : null;
        if (reqUserId instanceof Long l) {
            return l.equals(requestedVendorId);
        }
        if (reqUserId != null) {
            try {
                return Long.parseLong(reqUserId.toString()) == requestedVendorId;
            } catch (Exception ignored) { }
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        boolean isVendorRole = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(r -> r.equals("ROLE_VENDOR") || r.equals("ROLE_VENDOR_ADMIN"));
        if (!isVendorRole) return false;

        // If we have full vendor principal, compare by vendor id
        try {
            Object principal = auth.getPrincipal();
            if (principal instanceof CustomVendorDetails cvd && cvd.getVendor() != null) {
                Long principalVendorId = cvd.getVendor().getId();
                return principalVendorId != null && principalVendorId.equals(requestedVendorId);
            }
        } catch (Exception ignored) { }

        // Last-resort fallback: if name happens to be the numeric id
        try {
            String name = auth.getName();
            return name != null && name.equals(String.valueOf(requestedVendorId));
        } catch (Exception ignored) { }

        return false;
    }
}
