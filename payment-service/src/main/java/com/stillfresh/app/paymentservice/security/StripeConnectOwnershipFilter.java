package com.stillfresh.app.paymentservice.security;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.stillfresh.app.paymentservice.service.PaymentService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Restricts every Stripe Connect endpoint to the caller's own Connect account.
 *
 * <p>The endpoints under {@code /api/payment/stripe/connect} each accept an {@code accountId}
 * with no check that it belongs to the caller. That exposed any vendor's balance, payouts,
 * transactions and bank accounts to any other vendor, and
 * {@code POST /login-link/{accountId}} handed out a Stripe Express Dashboard session for an
 * arbitrary account.
 *
 * <p>The check lives in a filter rather than in each handler because the account ID appears in
 * different positions depending on the route, and a new endpoint added later would otherwise
 * silently ship unprotected. Stripe account IDs always carry the {@code acct_} prefix, so every
 * such value in the path or query string is validated regardless of where it appears.
 */
@Component
public class StripeConnectOwnershipFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(StripeConnectOwnershipFilter.class);

    private static final String CONNECT_PATH_PREFIX = "/api/payment/stripe/connect";
    private static final String STRIPE_ACCOUNT_PREFIX = "acct_";

    private final CallerContext callerContext;
    private final PaymentService paymentService;

    @Autowired
    public StripeConnectOwnershipFilter(CallerContext callerContext, @Lazy PaymentService paymentService) {
        this.callerContext = callerContext;
        this.paymentService = paymentService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path == null || !path.startsWith(CONNECT_PATH_PREFIX);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        Set<String> referencedAccounts = referencedAccountIds(request);
        if (referencedAccounts.isEmpty() || callerContext.isAdmin()) {
            chain.doFilter(request, response);
            return;
        }

        Long callerVendorId = callerContext.vendorId();
        String ownAccountId = paymentService.getVendorStripeAccountId(callerVendorId);

        if (ownAccountId == null || !referencedAccounts.equals(Set.of(ownAccountId))) {
            logger.warn("Rejected Stripe Connect call to {} by vendorId {}: requested {} but owns {}",
                    request.getRequestURI(), callerVendorId, referencedAccounts, ownAccountId);
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"You can only access your own Stripe account\"}");
            return;
        }

        chain.doFilter(request, response);
    }

    /** Collects every {@code acct_*} value in the path and query string. */
    private Set<String> referencedAccountIds(HttpServletRequest request) {
        Set<String> accounts = new LinkedHashSet<>();

        String path = request.getRequestURI();
        if (path != null) {
            for (String segment : path.split("/")) {
                if (segment.startsWith(STRIPE_ACCOUNT_PREFIX)) {
                    accounts.add(segment);
                }
            }
        }

        request.getParameterMap().forEach((name, values) -> {
            for (String value : values) {
                if (value != null && value.startsWith(STRIPE_ACCOUNT_PREFIX)) {
                    accounts.add(value);
                }
            }
        });

        return accounts;
    }
}
