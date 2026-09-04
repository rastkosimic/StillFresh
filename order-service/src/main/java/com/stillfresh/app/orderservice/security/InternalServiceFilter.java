package com.stillfresh.app.orderservice.security;

import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.stillfresh.app.sharedentities.security.InternalServiceHeaders;
import com.stillfresh.app.sharedentities.security.SharedSecret;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Authenticates vendor-service's calls to order-service's stats and internal endpoints.
 *
 * <p>{@code /orders/stats/**} returns per-vendor revenue aggregates and active orders, and
 * {@code /orders/internal/**} exposes order status and the customer's user ID. Both were
 * {@code permitAll()}, and the internal one only checked that an {@code X-Internal-Service}
 * header was non-empty, which any caller could satisfy. Ownership of the requested
 * {@code vendorId} is enforced by vendor-service before it calls here; this filter establishes
 * that the request came from a StillFresh service at all.
 */
@Component
public class InternalServiceFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(InternalServiceFilter.class);

    private static final List<String> INTERNAL_PATHS = List.of("/orders/stats/", "/orders/internal/");

    @Value("${internal.service.secret}")
    private String internalSecret;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        if (path != null && INTERNAL_PATHS.stream().anyMatch(path::startsWith)) {
            String provided = request.getHeader(InternalServiceHeaders.INTERNAL_SECRET);
            if (SharedSecret.matches(internalSecret, provided)) {
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        "internal-service", null,
                        List.of(new SimpleGrantedAuthority(InternalServiceHeaders.ROLE_INTERNAL_SERVICE)));
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } else {
                logger.warn("Rejected internal call to {} without a valid internal service secret from {}",
                        path, request.getRemoteAddr());
            }
        }

        chain.doFilter(request, response);
    }
}
