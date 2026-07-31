package com.stillfresh.app.orderservice.controller;

import com.stillfresh.app.orderservice.model.Order;
import com.stillfresh.app.orderservice.service.OrderService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@RequestMapping("/orders")
public class OrderController {

	@Autowired
    private OrderService orderService;

    private static boolean isSuperAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_SUPER_ADMIN"::equals);
    }

    private static boolean isVendorOrAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(r -> r.equals("ROLE_VENDOR") || r.equals("ROLE_VENDOR_ADMIN")
                            || r.equals("ROLE_ADMIN")  || r.equals("ROLE_SUPER_ADMIN"));
    }

    @PostMapping
    public Order createOrder(@RequestBody Order order) {
        return orderService.createOrder(order);
    }

    /**
     * List orders. Regular users see only their own orders; SUPER_ADMIN sees all.
     * Requires authentication. Optional: ?status=CANCELLED|COMPLETED|CONFIRMED|etc.
     */
    @GetMapping
    public ResponseEntity<?> getAllOrders(HttpServletRequest request,
                                          @RequestParam(name = "status", required = false) String status,
                                          @RequestParam(name = "page", required = false, defaultValue = "0") int page,
                                          @RequestParam(name = "size", required = false, defaultValue = "20") int size) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Authentication required"));
        }
        if (isSuperAdmin()) {
            if (status != null && !status.isBlank()) {
                Page<Order> pageResult = orderService.getOrdersByStatus(status, page, size);
                return ResponseEntity.ok(pageResult);
            }
            Page<Order> pageResult = orderService.getAllOrders(page, size);
            return ResponseEntity.ok(pageResult);
        }
        if (status != null && !status.isBlank()) {
            Page<Order> pageResult = orderService.getOrdersByUserIdAndStatus(userId, status, page, size);
            return ResponseEntity.ok(pageResult);
        }
        Page<Order> pageResult = orderService.getOrdersByUserId(userId, page, size);
        return ResponseEntity.ok(pageResult);
    }

    /**
     * Get one order by ID. Access is granted to:
     * - the customer who placed the order,
     * - a vendor whose location (vendorId) the order belongs to (e.g. when tapping an
     *   order notification),
     * - SUPER_ADMIN (any order).
     * Returns 404 when the order does not exist or the caller is not permitted to view it.
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getOrderById(HttpServletRequest request, @PathVariable Long id) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Authentication required"));
        }
        return orderService.getOrderById(id)
                .filter(order -> isSuperAdmin()
                        || order.getUserId().equals(userId)
                        || (isVendorOrAdmin() && order.getVendorId() != null
                            && order.getVendorId().equals(userId)))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteOrder(HttpServletRequest request, @PathVariable Long id) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Authentication required"));
        }
        if (!isSuperAdmin() && !orderService.isOrderOwnedByUser(id, userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Order not found or access denied"));
        }
        orderService.deleteOrder(id);
        return ResponseEntity.ok(Map.of("message", "Order deleted"));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateOrderStatus(@PathVariable Long id, @RequestBody UpdateOrderStatusRequest request) {
        if (!isVendorOrAdmin()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Only vendors and admins can update order status"));
        }
        return ResponseEntity.ok(orderService.updateOrderStatus(id, request.getStatus()));
    }

    /**
     * Customer confirms pickup for a manual-capture order (Stripe or AllSecure). Triggers payment capture
     * asynchronously via the active payment provider (order becomes COMPLETED when payment-service publishes
     * {@code PaymentCapturedEvent}).
     */
    @PutMapping("/{id}/confirm-pickup")
    public ResponseEntity<Map<String, Object>> confirmPickupByCustomer(
            HttpServletRequest request,
            @PathVariable Long id) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "Authentication required"));
        }
        if (!isSuperAdmin() && !orderService.isOrderOwnedByUser(id, userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("success", false, "message", "Order not found or access denied"));
        }
        Long effectiveUserId = isSuperAdmin()
                ? orderService.getOrderById(id).map(Order::getUserId).orElse(null)
                : userId;
        if (effectiveUserId == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "message", "Order not found"));
        }
        String err = orderService.requestCustomerPickupCapture(id, effectiveUserId);
        if (err != null) {
            return switch (err) {
                case "NOT_FOUND" -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("success", false, "message", "Order not found"));
                case "FORBIDDEN" -> ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("success", false, "message", "Order not found or access denied"));
                case "INVALID_STATUS" -> ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("success", false, "message", "Order cannot be confirmed for pickup in its current state"));
                case "NO_PAYMENT_REFERENCE" -> ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("success", false, "message", "This order has no authorized payment to capture at pickup"));
                default -> ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("success", false, "message", "Cannot confirm pickup"));
            };
        }
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Pickup confirmed. Payment capture has been requested."));
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<Map<String, Object>> cancelOrderByCustomer(
            HttpServletRequest request,
            @PathVariable Long id,
            @RequestBody(required = false) CancelOrderRequest requestBody) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "Authentication required"));
        }
        try {
            if (!isSuperAdmin() && !orderService.isOrderOwnedByUser(id, userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("success", false, "message", "Order not found or access denied"));
            }
            String reason = requestBody != null ? requestBody.getReason() : null;
            Double userLat = requestBody != null ? requestBody.getUserLat() : null;
            Double userLon = requestBody != null ? requestBody.getUserLon() : null;
            boolean success = orderService.cancelOrderByCustomer(id, reason, userLat, userLon);
            if (success) {
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Order cancelled successfully"
                ));
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", "Failed to cancel order"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "message", "Error: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}/reject")
    public ResponseEntity<Map<String, Object>> rejectOrderByVendor(
            @PathVariable Long id,
            @RequestBody(required = false) RejectOrderRequest request) {
        try {
            String reason = request != null ? request.getReason() : null;
            boolean success = orderService.rejectOrderByVendor(id, reason);
            if (success) {
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Order rejected successfully"
                ));
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", "Failed to reject order"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "message", "Error: " + e.getMessage()));
        }
    }

    // Inner classes for request bodies
    public static class UpdateOrderStatusRequest {
        private String status;

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }

    public static class CancelOrderRequest {
        private String reason;
        /** Optional device coordinates for the anti-bypass geo-fence check. */
        private Double userLat;
        private Double userLon;

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }

        public Double getUserLat() {
            return userLat;
        }

        public void setUserLat(Double userLat) {
            this.userLat = userLat;
        }

        public Double getUserLon() {
            return userLon;
        }

        public void setUserLon(Double userLon) {
            this.userLon = userLon;
        }
    }

    public static class RejectOrderRequest {
        private String reason;

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }
    }
}
