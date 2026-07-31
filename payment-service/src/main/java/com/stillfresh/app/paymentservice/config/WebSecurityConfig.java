package com.stillfresh.app.paymentservice.config;

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

import com.stillfresh.app.paymentservice.security.GatewayTrustFilter;

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
                .requestMatchers("/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**", "/swagger-ui/index.html").permitAll()  // Allow access to Swagger UI
                // AllSecure callback/return: no JWT, but GatewayTrustFilter requires X-Gateway-Secret (request must
                // transit the API gateway). Callback HMAC is verified inside AllSecureController.
                .requestMatchers("/payment/allsecure/callback", "/payment/allsecure/return").permitAll()
                .requestMatchers("/payment/**", "/admin/**", "/ledger/**", "/payment/bank-transfer/**").authenticated()
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        // Use GatewayTrustFilter - trusts API Gateway for authentication
        // Gateway validates JWT and adds X-* headers, this filter extracts user info from headers
        http.addFilterBefore(gatewayTrustFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
