package com.stillfresh.app.authorizationservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.stillfresh.app.sharedentities.security.InternalServiceHeaders;

import feign.RequestInterceptor;

@Configuration
public class VendorServiceFeignConfig {

    @Value("${internal.service.secret}")
    private String internalServiceSecret;

    /**
     * Presents the shared internal secret so vendor-service can verify the caller is a
     * StillFresh service.
     *
     * <p>This previously sent {@code X-Internal-Service: authorization-service}, a constant that
     * vendor-service only checked for being non-empty, so it authenticated nothing.
     */
    @Bean
    public RequestInterceptor requestInterceptor() {
        return requestTemplate ->
                requestTemplate.header(InternalServiceHeaders.INTERNAL_SECRET, internalServiceSecret);
    }
}
