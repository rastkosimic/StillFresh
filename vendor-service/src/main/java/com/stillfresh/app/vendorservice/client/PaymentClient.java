package com.stillfresh.app.vendorservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

@FeignClient(name = "payment-service", configuration = com.stillfresh.app.vendorservice.config.PaymentServiceFeignConfig.class)
public interface PaymentClient {
    
    /**
     * Response DTO for Stripe Connect operations
     */
    class StripeConnectResponse {
        private String value;

        public StripeConnectResponse() {
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }
    
    /**
     * Creates a Stripe Connect account for a vendor
     * @param vendorEmail Vendor's email address
     * @param vendorName Vendor's business name
     * @return Stripe Connect account ID wrapped in response object
     */
    @PostMapping("/api/payment/stripe/connect/create-account")
    StripeConnectResponse createConnectAccount(
        @RequestParam("vendorEmail") String vendorEmail,
        @RequestParam("vendorName") String vendorName
    );
    
    /**
     * Creates an account link for vendor onboarding
     * @param accountId Stripe Connect account ID
     * @return Onboarding URL wrapped in response object
     */
    @PostMapping("/api/payment/stripe/connect/create-account-link")
    StripeConnectResponse createAccountLink(@RequestParam("accountId") String accountId);
    
    /**
     * Checks if a Stripe Connect account is ready to receive payments
     * @param accountId Stripe Connect account ID
     * @return true if account is ready, false otherwise
     */
    @GetMapping("/api/payment/stripe/connect/account-ready/{accountId}")
    Boolean isAccountReady(@PathVariable("accountId") String accountId);
    
    /**
     * Retrieves detailed account information for a Stripe Connect account
     * @param accountId Stripe Connect account ID
     * @return Account details as a map
     */
    @GetMapping("/api/payment/stripe/connect/account/{accountId}")
    Map<String, Object> getAccountDetails(@PathVariable("accountId") String accountId);
    
    /**
     * Creates a login link for the Stripe Express Dashboard
     * @param accountId Stripe Connect account ID
     * @return Login URL wrapped in response object
     */
    @PostMapping("/api/payment/stripe/connect/login-link/{accountId}")
    StripeConnectResponse createLoginLink(@PathVariable("accountId") String accountId);
    
    /**
     * Retrieves payouts for a Stripe Connect account
     * @param accountId Stripe Connect account ID
     * @param limit Maximum number of payouts to retrieve
     * @return List of payouts as maps
     */
    @GetMapping("/api/payment/stripe/connect/payouts/{accountId}")
    List<Map<String, Object>> getPayouts(
        @PathVariable("accountId") String accountId,
        @RequestParam(value = "limit", required = false) Integer limit
    );
    
    /**
     * Retrieves a specific payout by ID
     * @param accountId Stripe Connect account ID
     * @param payoutId Payout ID
     * @return Payout details as a map
     */
    @GetMapping("/api/payment/stripe/connect/payouts/{accountId}/{payoutId}")
    Map<String, Object> getPayout(
        @PathVariable("accountId") String accountId,
        @PathVariable("payoutId") String payoutId
    );
    
    /**
     * Retrieves the balance for a Stripe Connect account
     * @param accountId Stripe Connect account ID
     * @return Balance information as a map
     */
    @GetMapping("/api/payment/stripe/connect/balance/{accountId}")
    Map<String, Object> getBalance(@PathVariable("accountId") String accountId);
    
    /**
     * Retrieves transaction history for a Stripe Connect account
     * @param accountId Stripe Connect account ID
     * @param limit Maximum number of transactions to retrieve
     * @return List of transactions as maps
     */
    @GetMapping("/api/payment/stripe/connect/transactions/{accountId}")
    List<Map<String, Object>> getTransactions(
        @PathVariable("accountId") String accountId,
        @RequestParam(value = "limit", required = false) Integer limit
    );
    
    /**
     * Retrieves verification requirements for a Stripe Connect account
     * @param accountId Stripe Connect account ID
     * @return Requirements details as a map
     */
    @GetMapping("/api/payment/stripe/connect/requirements/{accountId}")
    Map<String, Object> getRequirements(@PathVariable("accountId") String accountId);

    /** Unsettled ledger balance for a MoR vendor in minor currency units. */
    @GetMapping("/ledger/balance/{vendorId}")
    Map<String, Object> getVendorLedgerBalance(@PathVariable("vendorId") Long vendorId);

    /** Most recently completed payout item for a vendor. Returns null / 204 if none. */
    @GetMapping("/ledger/vendors/{vendorId}/last-payout")
    Map<String, Object> getLastPayout(@PathVariable("vendorId") Long vendorId);
}

