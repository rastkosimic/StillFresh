package com.stillfresh.app.authorizationservice.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import com.stillfresh.app.authorizationservice.security.InternalServiceFilter;
import com.stillfresh.app.authorizationservice.security.JwtRequestFilter;
import com.stillfresh.app.sharedentities.security.InternalServiceHeaders;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Autowired
    @Lazy
    private JwtRequestFilter jwtRequestFilter;

    @Autowired
    @Lazy
    private InternalServiceFilter internalServiceFilter;

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
                .requestMatchers(
                    "/auth/login", 
                    "/auth/register", 
                    "/auth/verify", 
                    "/auth/forgot-password", 
                    "/auth/reset-password", 
                    "/auth/refresh-token", 
                    "/auth/google-login",
                    "/auth/oauth2/**",  // OAuth2 endpoints
                    "/oauth2/**",  // Spring OAuth2 endpoints
                    "/auth/check-availability", 
                    // Unauthenticated by necessity: it runs before any admin exists. Guarded by
                    // the X-Admin-Bootstrap-Token check in AdminController, which is disabled
                    // unless admin.bootstrap.token is configured.
                    "/admin/create-initial-admin",
                    "/v3/api-docs/**", 
                    "/swagger-ui/**"
                ).permitAll()  // Open endpoints for authentication
                // Service-to-service credential, role and status management. These mint global
                // user IDs and mutate accounts, so they require the shared internal secret that
                // InternalServiceFilter verifies — never an end-user token.
                .requestMatchers("/api/auth/**")
                    .hasRole(InternalServiceHeaders.INTERNAL_SERVICE)
                .anyRequest().authenticated()  // Any other request requires authentication (including /auth/change-password)
            )
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));  // Stateless session management

        http.addFilterBefore(internalServiceFilter, UsernamePasswordAuthenticationFilter.class);
        http.addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);  // Add JWT filter before standard filters

        return http.build();
    }
}

