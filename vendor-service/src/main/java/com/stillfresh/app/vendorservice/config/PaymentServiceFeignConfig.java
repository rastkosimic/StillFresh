package com.stillfresh.app.vendorservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.stillfresh.app.sharedentities.security.InternalServiceHeaders;

import feign.RequestInterceptor;

/**
 * Presents the shared internal secret on payment-service ledger calls that are not reachable
 * with a gateway-stamped vendor identity.
 *
 * <p>{@code /ledger/balance/**} and last-payout under {@code /ledger/vendors/} require an
 * authenticated caller and an {@code isAdminOrSelf} check. Feign from vendor-service has
 * neither gateway headers nor a vendor role that payment-service can tie to a numeric
 * vendor ID. Ownership of the {@code vendorId} is enforced here in vendor-service before
 * the call is made; this header only proves the request came from a StillFresh service.
 */
@Configuration
public class PaymentServiceFeignConfig {

    @Value("${internal.service.secret}")
    private String internalServiceSecret;

    @Bean
    public RequestInterceptor paymentServiceInternalInterceptor() {
        return requestTemplate -> {
            String url = requestTemplate.url();
            if (url.contains("/ledger/balance/") || url.contains("/ledger/vendors/")) {
                requestTemplate.header(InternalServiceHeaders.INTERNAL_SECRET, internalServiceSecret);
            }
        };
    }
}
