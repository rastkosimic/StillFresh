package com.stillfresh.app.paymentservice.dto;

/**
 * Provider-agnostic result of a card registration request.
 *
 * <ul>
 *   <li>Stripe: {@code customerId} is populated, {@code redirectUrl} is null.</li>
 *   <li>AllSecure (hosted flow): {@code redirectUrl} is populated for the client to open;
 *       the card is persisted asynchronously via the callback.</li>
 * </ul>
 */
public class CardRegistrationResult {

    private final String provider;
    private final String customerId;
    private final String redirectUrl;
    private final String transactionId;
    private final String message;

    private CardRegistrationResult(String provider, String customerId, String redirectUrl,
                                   String transactionId, String message) {
        this.provider = provider;
        this.customerId = customerId;
        this.redirectUrl = redirectUrl;
        this.transactionId = transactionId;
        this.message = message;
    }

    public static CardRegistrationResult stripe(String customerId, String message) {
        return new CardRegistrationResult("stripe", customerId, null, null, message);
    }

    public static CardRegistrationResult redirect(String provider, String redirectUrl,
                                                  String transactionId, String message) {
        return new CardRegistrationResult(provider, null, redirectUrl, transactionId, message);
    }

    public String getProvider() { return provider; }
    public String getCustomerId() { return customerId; }
    public String getRedirectUrl() { return redirectUrl; }
    public String getTransactionId() { return transactionId; }
    public String getMessage() { return message; }
}
