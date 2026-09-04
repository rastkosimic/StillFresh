package com.stillfresh.app.authorizationservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.stillfresh.app.sharedentities.security.InternalServiceHeaders;

import feign.RequestInterceptor;

/**
 * Feign client configuration for user-service.
 * Uses Spring Cloud OpenFeign's default configuration with Jackson.
 */
@Configuration
public class UserServiceFeignConfig {
    // Using default Feign configuration provided by Spring Cloud OpenFeign
    // which includes Jackson encoder/decoder automatically

    @Value("${internal.service.secret}")
    private String internalServiceSecret;

    /**
     * OAuth2 registration creates the user account in user-service before the user has any
     * token of their own, so the shared internal secret is what authorizes the call.
     */
    @Bean
    public RequestInterceptor userServiceRequestInterceptor() {
        return requestTemplate ->
                requestTemplate.header(InternalServiceHeaders.INTERNAL_SECRET, internalServiceSecret);
    }
}
