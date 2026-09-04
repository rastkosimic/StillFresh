package com.stillfresh.app.orderservice.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.stillfresh.app.orderservice.security.GatewayTrustFilter;
import com.stillfresh.app.orderservice.security.InternalServiceFilter;
import com.stillfresh.app.sharedentities.security.InternalServiceHeaders;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

    @Autowired
    @Lazy
    private GatewayTrustFilter gatewayTrustFilter;

    @Autowired
    @Lazy
    private InternalServiceFilter internalServiceFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                // Called by vendor-service, which enforces vendorId ownership before calling.
                // These return revenue aggregates, delivery addresses and customer user IDs, so
                // they must not be reachable without the shared internal secret.
                .requestMatchers("/orders/stats/**", "/orders/internal/**")
                    .hasRole(InternalServiceHeaders.INTERNAL_SERVICE)
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session.sessionCreationPolicy(org.springframework.security.config.http.SessionCreationPolicy.STATELESS));

        // Use GatewayTrustFilter - trusts API Gateway for authentication
        // Gateway validates JWT and adds X-* headers, this filter extracts user info from headers
        http.addFilterBefore(internalServiceFilter, UsernamePasswordAuthenticationFilter.class);
        http.addFilterBefore(gatewayTrustFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}

