package com.stillfresh.app.notificationservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.stillfresh.app.notificationservice.config.InternalServiceFeignConfig;

import java.util.Map;

/**
 * Minimal client for resolving user contact details (email) from authorization-service,
 * which is the source of truth for user credentials. Used by the email-sending consumers
 * for events that only carry a userId (payments, bank transfers, cancellations).
 */
@FeignClient(name = "authorization-service", configuration = InternalServiceFeignConfig.class)
public interface AuthUserClient {

    /**
     * Returns user information for the given global user ID. The response body is
     * {@code { success, user: { id, email, username, role, status } }}.
     */
    @GetMapping("/api/auth/user/{globalUserId}")
    Map<String, Object> getUserByGlobalId(@PathVariable("globalUserId") Long globalUserId);
}
