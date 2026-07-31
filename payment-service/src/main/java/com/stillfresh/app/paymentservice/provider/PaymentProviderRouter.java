package com.stillfresh.app.paymentservice.provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.List;

/**
 * Selects the active {@link PaymentProvider} based on the {@code payment.provider} property
 * (default "stripe"). Lets Stripe and AllSecure run in parallel behind a flag.
 */
@Component
public class PaymentProviderRouter {

    private static final Logger logger = LoggerFactory.getLogger(PaymentProviderRouter.class);

    private final List<PaymentProvider> providers;

    @Value("${payment.provider:stripe}")
    private String activeProviderName;

    public PaymentProviderRouter(List<PaymentProvider> providers) {
        this.providers = providers;
    }

    @PostConstruct
    public void logActiveProvider() {
        logger.info("Active payment provider: {}", activeProviderName);
    }

    /** Returns the currently active provider, falling back to Stripe if the configured name is unknown. */
    public PaymentProvider active() {
        for (PaymentProvider provider : providers) {
            if (provider.name().equalsIgnoreCase(activeProviderName)) {
                return provider;
            }
        }
        logger.warn("Unknown payment.provider '{}'; falling back to '{}'.",
                activeProviderName, StripePaymentProvider.NAME);
        for (PaymentProvider provider : providers) {
            if (provider.name().equalsIgnoreCase(StripePaymentProvider.NAME)) {
                return provider;
            }
        }
        throw new IllegalStateException("No payment provider available");
    }

    public boolean isAllSecureActive() {
        return AllSecurePaymentProvider.NAME.equalsIgnoreCase(activeProviderName);
    }
}
