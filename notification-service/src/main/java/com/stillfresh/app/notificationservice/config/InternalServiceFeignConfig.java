package com.stillfresh.app.notificationservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.stillfresh.app.sharedentities.security.InternalServiceHeaders;

import feign.RequestInterceptor;

/**
 * Attaches the shared internal secret to Feign calls against {@code /api/auth/**}.
 *
 * <p>Notification consumers resolve a recipient's email from authorization-service while handling
 * a Kafka event, so there is no incoming HTTP request whose Authorization header could be
 * forwarded. The internal secret is what authorizes these lookups.
 */
@Configuration
public class InternalServiceFeignConfig {

    @Value("${internal.service.secret}")
    private String internalServiceSecret;

    @Bean
    public RequestInterceptor internalServiceRequestInterceptor() {
        return requestTemplate -> {
            if (requestTemplate.url().contains("/api/auth/")) {
                requestTemplate.header(InternalServiceHeaders.INTERNAL_SECRET, internalServiceSecret);
            }
        };
    }
}
