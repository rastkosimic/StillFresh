package com.stillfresh.app.vendorservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.stillfresh.app.sharedentities.security.InternalServiceHeaders;

import feign.RequestInterceptor;

@Configuration
public class OrderServiceFeignConfig {

    @Value("${internal.service.secret}")
    private String internalServiceSecret;

    /**
     * Presents the shared internal secret on order-service's stats and internal endpoints.
     *
     * <p>Previously these calls sent {@code X-Internal-Service: vendor-service}, which
     * order-service only checked for being non-empty — any caller could supply it. Ownership of
     * the {@code vendorId} is still enforced here in vendor-service before the call is made;
     * this header only proves the request came from a StillFresh service.
     */
    @Bean
    public RequestInterceptor orderServiceInternalInterceptor() {
        return requestTemplate -> {
            String url = requestTemplate.url();
            if (url.contains("/orders/internal/") || url.contains("/orders/stats/")) {
                requestTemplate.header(InternalServiceHeaders.INTERNAL_SECRET, internalServiceSecret);
            }
        };
    }
}
