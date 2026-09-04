package com.stillfresh.app.paymentservice.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Reads the authenticated caller's identity for the current request.
 *
 * <p>{@code GatewayTrustFilter} stamps the gateway-supplied user ID and email onto the request as
 * attributes, while the Spring Security principal name holds the username. Authorization checks
 * in this service need the numeric ID, so they must not compare the principal name against a
 * vendor ID.
 */
@Component
public class CallerContext {

    /**
     * The caller's vendor (or user) ID as issued by the gateway, or {@code null} when the request
     * carries no gateway identity.
     */
    public Long vendorId() {
        Object userId = requestAttribute("userId");
        if (userId instanceof Long id) {
            return id;
        }
        if (userId != null) {
            try {
                return Long.parseLong(userId.toString());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    /** The caller's email as issued by the gateway, or {@code null} if absent. */
    public String email() {
        Object email = requestAttribute("email");
        return email != null ? email.toString() : null;
    }

    public boolean isAdmin() {
        return hasAnyAuthority("ROLE_ADMIN", "ROLE_SUPER_ADMIN");
    }

    public boolean isVendor() {
        return hasAnyAuthority("ROLE_VENDOR", "ROLE_VENDOR_ADMIN");
    }

    private boolean hasAnyAuthority(String... authorities) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return false;
        }
        for (String authority : authorities) {
            boolean present = auth.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .anyMatch(authority::equals);
            if (present) {
                return true;
            }
        }
        return false;
    }

    private Object requestAttribute(String name) {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servletAttributes) {
            return servletAttributes.getRequest().getAttribute(name);
        }
        return null;
    }
}
