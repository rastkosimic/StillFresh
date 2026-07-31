package com.stillfresh.app.authorizationservice.config;

import org.springframework.context.annotation.Configuration;

/**
 * Feign client configuration for user-service.
 * Uses Spring Cloud OpenFeign's default configuration with Jackson.
 */
@Configuration
public class UserServiceFeignConfig {
    // Using default Feign configuration provided by Spring Cloud OpenFeign
    // which includes Jackson encoder/decoder automatically
}

