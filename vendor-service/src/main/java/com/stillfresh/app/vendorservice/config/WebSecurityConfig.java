package com.stillfresh.app.vendorservice.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import com.stillfresh.app.vendorservice.security.GatewayTrustFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class WebSecurityConfig {

    @Autowired
    @Lazy
    private GatewayTrustFilter gatewayTrustFilter;

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/auth/login", "/vendors/register", "/vendors/apply", "/vendors/verify", "/vendors/forgot-password", "/vendors/reset-password").permitAll()
                .requestMatchers("/admin/create-initial-admin").permitAll()
                .requestMatchers("/vendors/internal/**").permitAll()  // Internal service endpoints (protected by X-Internal-Service header check)
                .requestMatchers("/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**").permitAll()  // Allow access to Swagger
                .requestMatchers(HttpMethod.GET, "/vendors/uploads/**").permitAll()  // Public image serving
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        // Use GatewayTrustFilter - trusts API Gateway for authentication
        // Gateway validates JWT and adds X-* headers, this filter extracts user info from headers
        http.addFilterBefore(gatewayTrustFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}

