package com.stillfresh.app.vendorservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.stillfresh.app.sharedentities.security.InternalServiceHeaders;

import feign.RequestInterceptor;

/**
 * Presents the shared internal secret on offer-service calls that are not publicly readable.
 *
 * <p>{@code /offers/stats/**} exposes per-vendor supply figures and {@code /offers/*//*all-offers}
 * includes expired and sold-out listings. Ownership of the {@code vendorId} is enforced here in
 * vendor-service; this header only proves the request came from a StillFresh service.
 */
@Configuration
public class OfferServiceFeignConfig {

    @Value("${internal.service.secret}")
    private String internalServiceSecret;

    @Bean
    public RequestInterceptor offerServiceInternalInterceptor() {
        return requestTemplate -> {
            String url = requestTemplate.url();
            if (url.contains("/offers/stats/") || url.contains("/all-offers")) {
                requestTemplate.header(InternalServiceHeaders.INTERNAL_SECRET, internalServiceSecret);
            }
        };
    }
}
