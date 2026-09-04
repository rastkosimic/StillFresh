package com.stillfresh.app.offerservice.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.stillfresh.app.offerservice.security.GatewayTrustFilter;
import com.stillfresh.app.offerservice.security.InternalServiceFilter;
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
                // Per-vendor supply figures for an arbitrary vendorId. Internal only; matched
                // before the /offers/* patterns below, which would otherwise swallow it.
                .requestMatchers("/offers/stats/**")
                    .hasRole(InternalServiceHeaders.INTERNAL_SERVICE)

                // Marketplace browsing. Deliberately open: customers discover offers before
                // signing in, and these return only public listing data.
                .requestMatchers(HttpMethod.GET,
                        "/offers",
                        "/offers/nearby",
                        "/offers/categories",
                        "/offers/{id}",
                        "/offers/{vendorId}/active").permitAll()
                // Public listing data, fetched in bulk by user-service for favorites.
                .requestMatchers(HttpMethod.POST, "/offers/batch").permitAll()
                .requestMatchers("/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**").permitAll()

                // Includes expired and sold-out offers, so it needs a caller identity. Both an
                // end user via the gateway and vendor-service via the internal secret qualify.
                .requestMatchers(HttpMethod.GET, "/offers/{vendorId}/all-offers").authenticated()

                // Destructive: OfferService.deleteOffer additionally checks that the caller owns
                // the offer. Previously this was reachable unauthenticated.
                .requestMatchers(HttpMethod.DELETE, "/offers/{id}")
                    .hasAnyRole("VENDOR_ADMIN", "ADMIN", "SUPER_ADMIN")

                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        // Use GatewayTrustFilter - trusts API Gateway for authentication
        // Gateway validates JWT and adds X-* headers, this filter extracts user info from headers
        http.addFilterBefore(internalServiceFilter, UsernamePasswordAuthenticationFilter.class);
        http.addFilterBefore(gatewayTrustFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

}

