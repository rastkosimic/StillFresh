package com.stillfresh.app.paymentservice.provider;

import com.stillfresh.app.paymentservice.dto.CardRegistrationRequest;
import com.stillfresh.app.paymentservice.dto.CardRegistrationResult;
import com.stillfresh.app.paymentservice.dto.CustomerPaymentMethodDto;
import com.stillfresh.app.sharedentities.payment.events.PaymentRequestEvent;

import java.security.Principal;
import java.util.List;

/**
 * Abstraction over the active payment backend (Stripe or AllSecure), selected via {@code payment.provider}.
 *
 * <p>Card-management operations and the order lifecycle (preauthorize at order placement, capture at
 * pickup, cancel on expiry/cancellation) are routed through this interface so the two providers can run
 * in parallel behind a flag.</p>
 */
public interface PaymentProvider {

    /** Provider identifier, e.g. "stripe" or "allsecure". */
    String name();

    /** Registers a customer card. Result shape varies by provider (see {@link CardRegistrationResult}). */
    CardRegistrationResult registerCard(CardRegistrationRequest request, Principal principal);

    /** Lists the authenticated customer's stored payment methods. */
    List<CustomerPaymentMethodDto> listPaymentMethods(Principal principal);

    /** Deletes a stored payment method by its provider-specific id/reference. */
    void deletePaymentMethod(String paymentMethodId, Principal principal);

    /** Authorizes (places a hold) for an order placement payment request. */
    void preauthorize(PaymentRequestEvent event);

    /** Captures (settles) a previously authorized payment, identified by the stored reference. */
    void capture(String referenceId);

    /** Cancels/voids a previously authorized payment, releasing the hold. */
    void cancel(String referenceId);
}
