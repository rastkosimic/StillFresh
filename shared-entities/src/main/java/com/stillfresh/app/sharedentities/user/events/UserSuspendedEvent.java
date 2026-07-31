package com.stillfresh.app.sharedentities.user.events;

/**
 * Emitted by user-service when a user is auto-suspended (e.g. after reaching the strike threshold).
 * Consumed by authorization-service to block re-login and revoke active sessions.
 */
public class UserSuspendedEvent {

    private Long userId;
    private String reason;

    public UserSuspendedEvent() {
    }

    public UserSuspendedEvent(Long userId, String reason) {
        this.userId = userId;
        this.reason = reason;
    }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    @Override
    public String toString() {
        return "UserSuspendedEvent{userId=" + userId + ", reason='" + reason + "'}";
    }
}
