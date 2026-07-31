package com.stillfresh.app.vendorservice.config;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

@Configuration
public class FeignClientConfig {

    @Bean
    public RequestInterceptor bearerAuthRequestInterceptor() {
        return template -> {
            // Try to propagate Authorization from SecurityContext (set in JwtRequestFilter)
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getDetails() instanceof String) {
                String details = (String) auth.getDetails();
                if (details.startsWith("Bearer ")) {
                    template.header("Authorization", details);
                    return;
                }
            }

            // Fallback: read current HTTP request header
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                String authHeader = request.getHeader("Authorization");
                if (authHeader != null && !authHeader.isBlank()) {
                    template.header("Authorization", authHeader);
                }
            }
        };
    }
}

