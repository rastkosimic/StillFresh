package com.stillfresh.app.offerservice.security;

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
 * Authenticates vendor-service's calls to {@code /offers/stats/**} and
 * {@code GET /offers/{vendorId}/all-offers}.
 *
 * <p>Stats return per-vendor supply figures; all-offers includes expired and sold-out
 * listings. Both were covered by the blanket {@code /offers/**} {@code permitAll()}, so any
 * caller could read any vendor's data. Ownership of the {@code vendorId} is enforced by
 * vendor-service before it calls here.
 */
@Component
public class InternalServiceFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(InternalServiceFilter.class);

    private static final String STATS_PREFIX = "/offers/stats/";
    private static final String ALL_OFFERS_SUFFIX = "/all-offers";

    @Value("${internal.service.secret}")
    private String internalSecret;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        if (isInternalPath(path)) {
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

    private static boolean isInternalPath(String path) {
        return path != null && (path.startsWith(STATS_PREFIX) || path.endsWith(ALL_OFFERS_SUFFIX));
    }
}
