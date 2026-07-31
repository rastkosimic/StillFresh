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

@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

    @Autowired
    @Lazy
    private GatewayTrustFilter gatewayTrustFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/orders/stats/**").permitAll()
                .requestMatchers("/orders/internal/**").permitAll()
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session.sessionCreationPolicy(org.springframework.security.config.http.SessionCreationPolicy.STATELESS));

        // Use GatewayTrustFilter - trusts API Gateway for authentication
        // Gateway validates JWT and adds X-* headers, this filter extracts user info from headers
        http.addFilterBefore(gatewayTrustFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}

