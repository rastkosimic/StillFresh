package com.stillfresh.app.notificationservice.controller;

import com.stillfresh.app.notificationservice.dto.ApiResponse;
import com.stillfresh.app.notificationservice.model.FcmTokenEntity;
import com.stillfresh.app.notificationservice.model.NotificationEntity;
import com.stillfresh.app.notificationservice.model.NotificationPreferencesEntity;
import com.stillfresh.app.notificationservice.service.FcmTokenService;
import com.stillfresh.app.notificationservice.service.NotificationService;
import com.stillfresh.app.notificationservice.service.NotificationPreferencesService;
import com.stillfresh.app.notificationservice.util.JwtUtil;
import com.stillfresh.app.sharedentities.enums.NotificationType;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private static final Logger logger = LoggerFactory.getLogger(NotificationController.class);

    @Autowired
    private FcmTokenService fcmTokenService;
    
    @Autowired
    private NotificationService notificationService;
    
    @Autowired
    private NotificationPreferencesService preferencesService;
    
    @Autowired(required = false)  // Optional - only needed for backward compatibility with direct service calls
    private JwtUtil jwtUtil;

    /**
     * Extract userId from request attributes (set by GatewayTrustFilter) or fallback to JWT parsing.
     * This method prioritizes gateway headers over direct JWT validation for centralized authentication.
     */
    private String extractUserId(HttpServletRequest request, String authHeader) {
        // First, try to get userId from request attributes (set by GatewayTrustFilter)
        String userId = (String) request.getAttribute("userId");
        if (userId != null && !userId.isEmpty()) {
            logger.debug("Extracted userId from request attributes: {}", userId);
            return userId;
        }

        // Fallback: Parse from JWT token (for backward compatibility with direct service calls)
        if (authHeader != null && jwtUtil != null) {
            try {
                userId = jwtUtil.extractUserId(authHeader);
                logger.debug("Extracted userId from JWT token: {}", userId);
                return userId;
            } catch (Exception e) {
                logger.warn("Failed to extract userId from JWT token: {}", e.getMessage());
                throw new IllegalArgumentException("Invalid JWT token: " + e.getMessage());
            }
        }

        throw new IllegalArgumentException("Unable to extract userId: missing authentication information");
    }

    // FCM Token Management
    @PostMapping("/fcm-token/register")
    public ResponseEntity<ApiResponse<FcmTokenEntity>> registerToken(
            HttpServletRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam String token) {
        try {
            String userId = extractUserId(request, authHeader);
            return fcmTokenService.registerToken(userId, token);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Invalid authentication: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to register FCM token: " + e.getMessage()));
        }
    }

    @GetMapping("/fcm-token")
    public ResponseEntity<ApiResponse<FcmTokenEntity>> getToken(
            HttpServletRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            String userId = extractUserId(request, authHeader);
            return fcmTokenService.getToken(userId);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Invalid authentication: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to retrieve FCM token: " + e.getMessage()));
        }
    }

    @DeleteMapping("/fcm-token")
    public ResponseEntity<ApiResponse<Void>> deleteToken(
            HttpServletRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            String userId = extractUserId(request, authHeader);
            return fcmTokenService.deleteToken(userId);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Invalid authentication: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to delete FCM token: " + e.getMessage()));
        }
    }
    
    // Notification Management
    @GetMapping("/user")
    public ResponseEntity<ApiResponse<List<NotificationEntity>>> getUserNotifications(
            HttpServletRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            String userId = extractUserId(request, authHeader);
            List<NotificationEntity> notifications = notificationService.getUserNotifications(userId);
            return ResponseEntity.ok(ApiResponse.success("Notifications retrieved successfully", notifications));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Invalid authentication: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to retrieve notifications: " + e.getMessage()));
        }
    }
    
    @GetMapping("/user/unread")
    public ResponseEntity<ApiResponse<List<NotificationEntity>>> getUnreadNotifications(
            HttpServletRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            String userId = extractUserId(request, authHeader);
            List<NotificationEntity> notifications = notificationService.getUnreadNotifications(userId);
            return ResponseEntity.ok(ApiResponse.success("Unread notifications retrieved successfully", notifications));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Invalid authentication: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to retrieve unread notifications: " + e.getMessage()));
        }
    }
    
    @PostMapping("/mark-read/{notificationId}")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            HttpServletRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable String notificationId) {
        try {
            String userId = extractUserId(request, authHeader);
            // Verify the notification belongs to the user
            if (notificationService.isNotificationOwnedByUser(notificationId, userId)) {
                notificationService.markAsRead(notificationId);
                return ResponseEntity.ok(ApiResponse.success("Notification marked as read"));
            } else {
                return ResponseEntity.badRequest().body(ApiResponse.error("Notification not found or access denied"));
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Invalid authentication: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to mark notification as read: " + e.getMessage()));
        }
    }

    /**
     * Soft-delete a notification (removes it from the user's inbox). Only the owner can delete.
     */
    @DeleteMapping("/{notificationId}")
    public ResponseEntity<ApiResponse<Void>> deleteNotification(
            HttpServletRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable String notificationId) {
        try {
            String userId = extractUserId(request, authHeader);
            if (notificationService.deleteNotification(notificationId, userId)) {
                return ResponseEntity.ok(ApiResponse.success("Notification deleted"));
            } else {
                return ResponseEntity.badRequest().body(ApiResponse.error("Notification not found or access denied"));
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Invalid authentication: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to delete notification: " + e.getMessage()));
        }
    }
    
    @PostMapping("/mark-all-read")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(
            HttpServletRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            String userId = extractUserId(request, authHeader);
            notificationService.markAllAsRead(userId);
            return ResponseEntity.ok(ApiResponse.success("All notifications marked as read"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Invalid authentication: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to mark all notifications as read: " + e.getMessage()));
        }
    }
    
    // Notification Preferences
    @GetMapping("/preferences")
    public ResponseEntity<ApiResponse<NotificationPreferencesEntity>> getPreferences(
            HttpServletRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            String userId = extractUserId(request, authHeader);
            // Create defaults on first read so the app always receives a fully populated
            // preferences object (all types enabled, push on) instead of null.
            NotificationPreferencesEntity preferences = preferencesService.getOrCreatePreferences(userId);
            return ResponseEntity.ok(ApiResponse.success("Preferences retrieved successfully", preferences));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(401).body(ApiResponse.error("Invalid authentication: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to retrieve preferences: " + e.getMessage()));
        }
    }
    
    /**
     * Update the authenticated user's notification preferences.
     *
     * <p>Request body (mobile contract):
     * <pre>
     * {
     *   "pushEnabled": true,
     *   "emailEnabled": false,
     *   "smsEnabled": false,
     *   "enabledTypes": ["ORDER_CONFIRMED", "PAYMENT_FAILED", ...]
     * }
     * </pre>
     *
     * <p>Validation rules:
     * <ul>
     *   <li>{@code enabledTypes} values are uppercase {@link NotificationType} enum names. Unknown
     *       values are rejected with HTTP 400 and a message naming the offending value(s).</li>
     *   <li>{@code enabledTypes} may be empty or omitted (treated as an empty set).</li>
     *   <li>{@code pushEnabled: false} may be combined with a non-empty {@code enabledTypes}; the
     *       types are preserved so re-enabling push restores them.</li>
     *   <li>Any valid type is accepted regardless of the caller's role (no role-based filtering).</li>
     * </ul>
     */
    @PostMapping("/preferences")
    public ResponseEntity<ApiResponse<NotificationPreferencesEntity>> updatePreferences(
            HttpServletRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody Map<String, Object> requestBody) {
        String userId;
        try {
            userId = extractUserId(request, authHeader);
        } catch (IllegalArgumentException e) {
            // Authentication problems are a 401 concern, not a 400 validation failure.
            return ResponseEntity.status(401).body(ApiResponse.error("Invalid authentication: " + e.getMessage()));
        }

        try {
            Set<NotificationType> enabledTypes = parseEnabledTypes(requestBody.get("enabledTypes"));
            boolean pushEnabled = parseBoolean(requestBody.get("pushEnabled"), true);
            boolean emailEnabled = parseBoolean(requestBody.get("emailEnabled"), false);
            boolean smsEnabled = parseBoolean(requestBody.get("smsEnabled"), false);

            NotificationPreferencesEntity preferences = preferencesService.updatePreferences(
                userId, enabledTypes, pushEnabled, emailEnabled, smsEnabled);

            return ResponseEntity.ok(ApiResponse.success("Preferences updated", preferences));
        } catch (IllegalArgumentException e) {
            logger.warn("Rejected preferences update for user {}: {}", userId, e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to update preferences for user {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to update preferences: " + e.getMessage()));
        }
    }

    /**
     * Convert the raw JSON {@code enabledTypes} value (a list of strings) into a typed set.
     * Throws {@link IllegalArgumentException} with a specific message when an unknown value is found.
     */
    private Set<NotificationType> parseEnabledTypes(Object raw) {
        Set<NotificationType> result = new LinkedHashSet<>();
        if (raw == null) {
            return result;
        }
        if (!(raw instanceof Iterable<?>)) {
            throw new IllegalArgumentException("'enabledTypes' must be an array of notification type strings");
        }
        List<String> invalid = new ArrayList<>();
        for (Object item : (Iterable<?>) raw) {
            if (item == null) {
                continue;
            }
            String name = item.toString().trim();
            if (name.isEmpty()) {
                continue;
            }
            try {
                result.add(NotificationType.valueOf(name.toUpperCase()));
            } catch (IllegalArgumentException ex) {
                invalid.add(name);
            }
        }
        if (!invalid.isEmpty()) {
            throw new IllegalArgumentException("Unknown notification type(s): " + String.join(", ", invalid));
        }
        return result;
    }

    private boolean parseBoolean(Object value, boolean defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Boolean b) {
            return b;
        }
        return Boolean.parseBoolean(value.toString());
    }
    
    // Test notification endpoint
    @PostMapping("/test")
    public ResponseEntity<ApiResponse<Void>> sendTestNotification(
            HttpServletRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            String userId = extractUserId(request, authHeader);
            notificationService.sendTestNotification(userId);
            return ResponseEntity.ok(ApiResponse.success("Test notification sent"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Invalid authentication: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to send test notification: " + e.getMessage()));
        }
    }
}
