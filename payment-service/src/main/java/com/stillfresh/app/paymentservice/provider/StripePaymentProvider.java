package com.stillfresh.app.paymentservice.provider;

import com.stillfresh.app.paymentservice.dto.CardRegistrationRequest;
import com.stillfresh.app.paymentservice.dto.CardRegistrationResponse;
import com.stillfresh.app.paymentservice.dto.CardRegistrationResult;
import com.stillfresh.app.paymentservice.dto.CustomerPaymentMethodDto;
import com.stillfresh.app.paymentservice.service.PaymentService;
import com.stillfresh.app.sharedentities.payment.events.PaymentRequestEvent;
import com.stripe.exception.StripeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.List;

/**
 * {@link PaymentProvider} backed by the existing Stripe {@link PaymentService}. This is a thin adapter:
 * all Stripe behaviour is delegated unchanged.
 */
@Component
public class StripePaymentProvider implements PaymentProvider {

    private static final Logger logger = LoggerFactory.getLogger(StripePaymentProvider.class);

    public static final String NAME = "stripe";

    @Autowired
    private PaymentService paymentService;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public CardRegistrationResult registerCard(CardRegistrationRequest request, Principal principal) {
        CardRegistrationResponse response = paymentService.registerCard(request, principal);
        return CardRegistrationResult.stripe(response.getCustomerId(), response.getMessage());
    }

    @Override
    public List<CustomerPaymentMethodDto> listPaymentMethods(Principal principal) {
        return paymentService.getCustomerPaymentMethods(principal);
    }

    @Override
    public void deletePaymentMethod(String paymentMethodId, Principal principal) {
        paymentService.deletePaymentMethod(paymentMethodId, principal);
    }

    @Override
    public void preauthorize(PaymentRequestEvent event) {
        paymentService.processPaymentRequest(event);
    }

    @Override
    public void capture(String referenceId) {
        try {
            paymentService.capturePaymentIntent(referenceId);
        } catch (StripeException e) {
            logger.error("Stripe capture failed for paymentIntentId {}: {}", referenceId, e.getMessage(), e);
        } catch (RuntimeException e) {
            logger.warn("Stripe capture not applied for paymentIntentId {}: {}", referenceId, e.getMessage());
        }
    }

    @Override
    public void cancel(String referenceId) {
        try {
            paymentService.cancelPaymentIntent(referenceId);
        } catch (StripeException e) {
            logger.error("Stripe cancel failed for paymentIntentId {}: {}", referenceId, e.getMessage(), e);
        } catch (RuntimeException e) {
            logger.warn("Stripe cancel not applied for paymentIntentId {}: {}", referenceId, e.getMessage());
        }
    }
}
