package com.stillfresh.app.vendorservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import feign.RequestInterceptor;

@Configuration
public class OrderServiceFeignConfig {

    @Bean
    public RequestInterceptor orderServiceInternalInterceptor() {
        return requestTemplate -> {
            if (requestTemplate.url().contains("/orders/internal/")) {
                requestTemplate.header("X-Internal-Service", "vendor-service");
            }
        };
    }
}
