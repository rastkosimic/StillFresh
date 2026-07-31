package com.stillfresh.app.authorizationservice.config;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Feign configuration for vendor-service client
 */
@Configuration
public class VendorServiceFeignConfig {

    /**
     * Adds internal service header to requests
     * This allows vendor-service to identify internal service calls
     */
    @Bean
    public RequestInterceptor requestInterceptor() {
        return requestTemplate -> {
            requestTemplate.header("X-Internal-Service", "authorization-service");
        };
    }
}

