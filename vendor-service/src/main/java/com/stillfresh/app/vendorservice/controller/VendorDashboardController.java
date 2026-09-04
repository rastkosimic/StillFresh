package com.stillfresh.app.vendorservice.controller;

import com.stillfresh.app.vendorservice.dto.VendorDashboardResponse;
import com.stillfresh.app.vendorservice.model.Vendor;
import com.stillfresh.app.vendorservice.service.VendorDashboardService;
import com.stillfresh.app.vendorservice.service.VendorService;
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

    @Autowired
    private VendorService vendorService;

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

        // Only short-circuit on a positive self-match; a mismatched gateway userId must
        // still allow HQ chain admins to load a sibling location's dashboard.
        Object reqUserId = request != null ? request.getAttribute("userId") : null;
        if (reqUserId instanceof Long l && l.equals(requestedVendorId)) {
            return true;
        }
        if (reqUserId != null) {
            try {
                if (Long.parseLong(reqUserId.toString()) == requestedVendorId) {
                    return true;
                }
            } catch (Exception ignored) { }
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        boolean isVendorRole = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(r -> r.equals("ROLE_VENDOR") || r.equals("ROLE_VENDOR_ADMIN"));
        if (!isVendorRole) return false;

        if (canHeadquartersAccessChainLocation(requestedVendorId, request, auth)) {
            return true;
        }

        // Last-resort fallback: if name happens to be the numeric id
        try {
            String name = auth.getName();
            return name != null && name.equals(String.valueOf(requestedVendorId));
        } catch (Exception ignored) { }

        return false;
    }

    /** HQ VENDOR_ADMIN may view dashboards for any selling location in their chain. */
    private boolean canHeadquartersAccessChainLocation(
            Long requestedVendorId, HttpServletRequest request, Authentication auth) {
        try {
            Vendor caller = resolveCallerVendor(request, auth);
            if (caller == null
                    || !Boolean.TRUE.equals(caller.getIsHeadquarters())
                    || !Boolean.TRUE.equals(caller.getIsChainLocation())
                    || caller.getChainId() == null) {
                return false;
            }
            return vendorService.getVendorById(requestedVendorId)
                    .filter(target -> caller.getChainId().equals(target.getChainId())
                            && target.getAssignedLocationId() == null)
                    .isPresent();
        } catch (Exception ignored) {
            return false;
        }
    }

    /**
     * GatewayTrustFilter treats VENDOR_ADMIN as an "admin" (the role name contains "ADMIN"),
     * so the principal is a bare username rather than CustomVendorDetails. Fall back to the
     * gateway-provided user id and email to load the calling vendor.
     */
    private Vendor resolveCallerVendor(HttpServletRequest request, Authentication auth) {
        Object principal = auth.getPrincipal();
        if (principal instanceof CustomVendorDetails cvd && cvd.getVendor() != null) {
            return cvd.getVendor();
        }
        if (request == null) return null;

        Object reqUserId = request.getAttribute("userId");
        Long callerId = null;
        if (reqUserId instanceof Number n) {
            callerId = n.longValue();
        } else if (reqUserId != null) {
            try {
                callerId = Long.parseLong(reqUserId.toString());
            } catch (NumberFormatException ignored) { }
        }
        if (callerId != null) {
            Vendor byId = vendorService.getVendorById(callerId).orElse(null);
            if (byId != null) return byId;
        }

        Object email = request.getAttribute("email");
        if (email != null) {
            return vendorService.findByEmail(email.toString()).orElse(null);
        }
        return null;
    }
}
