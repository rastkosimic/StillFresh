package com.stillfresh.app.vendorservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.stillfresh.app.sharedentities.config.CustomErrorDecoder;
import com.stillfresh.app.sharedentities.security.InternalServiceHeaders;

import feign.RequestInterceptor;
import feign.codec.ErrorDecoder;
import feign.codec.Encoder;
import feign.codec.Decoder;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
public class AuthorizationServiceFeignConfig {

    @Value("${internal.service.secret}")
    private String internalServiceSecret;

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return objectMapper;
    }

    @Bean
    public Encoder feignEncoder(ObjectMapper objectMapper) {
        return new JacksonEncoder(objectMapper);
    }

    @Bean
    public Decoder feignDecoder(ObjectMapper objectMapper) {
        return new JacksonDecoder(objectMapper);
    }

    @Bean
    public RequestInterceptor requestInterceptor() {
        return requestTemplate -> {
            requestTemplate.header("Content-Type", "application/json");
            
            // Only add Authorization header for endpoints that require it
            String url = requestTemplate.url();
            if (url.contains("/api/auth/") && !url.contains("/check-availability")) {
                // These endpoints run during registration, before any user is authenticated, so
                // the shared internal secret — not a forwarded JWT — is what authorizes them.
                requestTemplate.header(InternalServiceHeaders.INTERNAL_SECRET, internalServiceSecret);

                // Still forward the caller's token when one exists, for audit context.
                ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                if (attributes != null) {
                    HttpServletRequest request = attributes.getRequest();
                    String authorizationHeader = request.getHeader("Authorization");
                    if (authorizationHeader != null && !authorizationHeader.isBlank()) {
                        requestTemplate.header("Authorization", authorizationHeader);
                    }
                }
            }
            // For public endpoints like /auth/check-availability, don't add Authorization header
        };
    }
}
