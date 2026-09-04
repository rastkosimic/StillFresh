package com.stillfresh.app.paymentservice.controller;

import com.stillfresh.app.paymentservice.dto.StripeAccountDetailsDto;
import com.stillfresh.app.paymentservice.dto.StripeBalanceDto;
import com.stillfresh.app.paymentservice.dto.StripeBankAccountDto;
import com.stillfresh.app.paymentservice.dto.StripeConnectResponse;
import com.stillfresh.app.paymentservice.dto.StripePayoutDto;
import com.stillfresh.app.paymentservice.dto.StripeRequirementsDto;
import com.stillfresh.app.paymentservice.dto.StripeTransactionDto;
import com.stillfresh.app.paymentservice.security.CallerContext;
import com.stillfresh.app.paymentservice.service.PaymentService;
import com.stillfresh.app.paymentservice.service.StripeConnectService;
import com.stripe.exception.StripeException;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payment/stripe/connect")
public class StripeConnectController {

    private static final Logger logger = LoggerFactory.getLogger(StripeConnectController.class);

    @Autowired
    private StripeConnectService stripeConnectService;

    @Autowired
    private CallerContext callerContext;

    @Autowired
    private PaymentService paymentService;

    private boolean callerMayAccessAccount(String accountId) {
        if (callerContext.isAdmin()) {
            return true;
        }
        Long vendorId = callerContext.vendorId();
        if (vendorId == null || accountId == null) {
            return false;
        }
        String ownedAccountId = paymentService.getVendorStripeAccountId(vendorId);
        return accountId.equals(ownedAccountId);
    }

    private <T> ResponseEntity<T> forbiddenAccountAccess() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    /**
     * Creates a Stripe Connect account for a vendor
     * @param vendorEmail Vendor's email address
     * @param vendorName Vendor's business name
     * @return Stripe Connect account ID
     */
    @PostMapping(value = "/create-account", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<StripeConnectResponse> createConnectAccount(
            @RequestParam("vendorEmail") String vendorEmail,
            @RequestParam("vendorName") String vendorName) {
        // A vendor may only onboard itself. Without this, a vendor could create a Connect
        // account under another business's email address and complete onboarding for it.
        if (!callerContext.isAdmin()) {
            String callerEmail = callerContext.email();
            if (callerEmail == null || !callerEmail.equalsIgnoreCase(vendorEmail)) {
                logger.warn("Rejected Stripe Connect account creation for a different vendor email");
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new StripeConnectResponse("You can only create a Stripe account for your own vendor"));
            }
        }
        try {
            logger.info("Creating Stripe Connect account for vendor: {}", vendorName);
            String accountId = stripeConnectService.createConnectAccount(vendorEmail, vendorName);
            return ResponseEntity.ok(new StripeConnectResponse(accountId));
        } catch (StripeException e) {
            logger.error("Failed to create Stripe Connect account for vendor: {}. Error: {}", vendorName, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new StripeConnectResponse("Failed to create Stripe Connect account: " + e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error creating Stripe Connect account: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new StripeConnectResponse("Unexpected error: " + e.getMessage()));
        }
    }

    /**
     * Creates an account link for vendor onboarding
     * @param accountId Stripe Connect account ID
     * @return Onboarding URL
     */
    @PostMapping(value = "/create-account-link", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<StripeConnectResponse> createAccountLink(@RequestParam("accountId") String accountId) {
        if (!callerMayAccessAccount(accountId)) {
            return forbiddenAccountAccess();
        }
        try {
            logger.info("Creating account link for Stripe account: {}", accountId);
            String onboardingUrl = stripeConnectService.createAccountLink(accountId);
            return ResponseEntity.ok(new StripeConnectResponse(onboardingUrl));
        } catch (StripeException e) {
            logger.error("Failed to create account link for account: {}. Error: {}", accountId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new StripeConnectResponse("Failed to create account link: " + e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error creating account link: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new StripeConnectResponse("Unexpected error: " + e.getMessage()));
        }
    }

    /**
     * Checks if a Stripe Connect account is ready to receive payments
     * @param accountId Stripe Connect account ID
     * @return true if account is ready, false otherwise
     */
    @GetMapping("/account-ready/{accountId}")
    public ResponseEntity<Boolean> isAccountReady(@PathVariable String accountId) {
        if (!callerMayAccessAccount(accountId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(false);
        }
        try {
            boolean ready = stripeConnectService.isAccountReady(accountId);
            return ResponseEntity.ok(ready);
        } catch (StripeException e) {
            logger.error("Failed to check account status for: {}. Error: {}", accountId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(false);
        } catch (Exception e) {
            logger.error("Unexpected error checking account status: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(false);
        }
    }

    /**
     * Retrieves detailed account information for a Stripe Connect account
     * @param accountId Stripe Connect account ID
     * @return Account details
     */
    @GetMapping("/account/{accountId}")
    public ResponseEntity<?> getAccountDetails(@PathVariable String accountId) {
        if (!callerMayAccessAccount(accountId)) {
            return forbiddenAccountAccess();
        }
        try {
            logger.info("Retrieving account details for Stripe account: {}", accountId);
            StripeAccountDetailsDto accountDetails = stripeConnectService.getAccountDetails(accountId);
            return ResponseEntity.ok(accountDetails);
        } catch (StripeException e) {
            logger.error("Failed to retrieve account details for: {}. Error: {}", accountId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new StripeConnectResponse("Failed to retrieve account details: " + e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error retrieving account details: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new StripeConnectResponse("Unexpected error: " + e.getMessage()));
        }
    }

    /**
     * Creates a login link for the Stripe Express Dashboard
     * @param accountId Stripe Connect account ID
     * @return Login URL for the Stripe dashboard
     */
    @PostMapping(value = "/login-link/{accountId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<StripeConnectResponse> createLoginLink(@PathVariable String accountId) {
        if (!callerMayAccessAccount(accountId)) {
            return forbiddenAccountAccess();
        }
        try {
            logger.info("Creating login link for Stripe account: {}", accountId);
            String loginUrl = stripeConnectService.createLoginLink(accountId);
            return ResponseEntity.ok(new StripeConnectResponse(loginUrl));
        } catch (StripeException e) {
            logger.error("Failed to create login link for account: {}. Error: {}", accountId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new StripeConnectResponse("Failed to create login link: " + e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error creating login link: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new StripeConnectResponse("Unexpected error: " + e.getMessage()));
        }
    }

    /**
     * Retrieves payouts for a Stripe Connect account
     * @param accountId Stripe Connect account ID
     * @param limit Maximum number of payouts to retrieve (default: 10, max: 100)
     * @return List of payouts
     */
    @GetMapping("/payouts/{accountId}")
    public ResponseEntity<?> getPayouts(
            @PathVariable String accountId,
            @RequestParam(value = "limit", required = false) Integer limit) {
        if (!callerMayAccessAccount(accountId)) {
            return forbiddenAccountAccess();
        }
        try {
            logger.info("Retrieving payouts for Stripe account: {} (limit: {})", accountId, limit);
            List<StripePayoutDto> payouts = stripeConnectService.getPayouts(accountId, limit);
            return ResponseEntity.ok(payouts);
        } catch (StripeException e) {
            logger.error("Failed to retrieve payouts for account: {}. Error: {}", accountId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new StripeConnectResponse("Failed to retrieve payouts: " + e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error retrieving payouts: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new StripeConnectResponse("Unexpected error: " + e.getMessage()));
        }
    }

    /**
     * Retrieves a specific payout by ID
     * @param accountId Stripe Connect account ID
     * @param payoutId Payout ID
     * @return Payout details
     */
    @GetMapping("/payouts/{accountId}/{payoutId}")
    public ResponseEntity<?> getPayout(
            @PathVariable String accountId,
            @PathVariable String payoutId) {
        if (!callerMayAccessAccount(accountId)) {
            return forbiddenAccountAccess();
        }
        try {
            logger.info("Retrieving payout {} for Stripe account: {}", payoutId, accountId);
            StripePayoutDto payout = stripeConnectService.getPayout(accountId, payoutId);
            return ResponseEntity.ok(payout);
        } catch (StripeException e) {
            logger.error("Failed to retrieve payout {} for account: {}. Error: {}", payoutId, accountId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new StripeConnectResponse("Failed to retrieve payout: " + e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error retrieving payout: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new StripeConnectResponse("Unexpected error: " + e.getMessage()));
        }
    }

    /**
     * Retrieves the balance for a Stripe Connect account
     * @param accountId Stripe Connect account ID
     * @return Balance information
     */
    @GetMapping("/balance/{accountId}")
    public ResponseEntity<?> getBalance(@PathVariable String accountId) {
        if (!callerMayAccessAccount(accountId)) {
            return forbiddenAccountAccess();
        }
        try {
            logger.info("Retrieving balance for Stripe account: {}", accountId);
            StripeBalanceDto balance = stripeConnectService.getBalance(accountId);
            return ResponseEntity.ok(balance);
        } catch (StripeException e) {
            logger.error("Failed to retrieve balance for account: {}. Error: {}", accountId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new StripeConnectResponse("Failed to retrieve balance: " + e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error retrieving balance: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new StripeConnectResponse("Unexpected error: " + e.getMessage()));
        }
    }

    /**
     * Retrieves verification requirements for a Stripe Connect account
     * @param accountId Stripe Connect account ID
     * @return Requirements details
     */
    @GetMapping("/requirements/{accountId}")
    public ResponseEntity<?> getRequirements(@PathVariable String accountId) {
        if (!callerMayAccessAccount(accountId)) {
            return forbiddenAccountAccess();
        }
        try {
            logger.info("Retrieving requirements for Stripe account: {}", accountId);
            StripeRequirementsDto requirements = stripeConnectService.getRequirements(accountId);
            return ResponseEntity.ok(requirements);
        } catch (StripeException e) {
            logger.error("Failed to retrieve requirements for account: {}. Error: {}", accountId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new StripeConnectResponse("Failed to retrieve requirements: " + e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error retrieving requirements: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new StripeConnectResponse("Unexpected error: " + e.getMessage()));
        }
    }

    /**
     * Lists all bank accounts for a Stripe Connect account
     * @param accountId Stripe Connect account ID
     * @return List of bank accounts
     */
    @GetMapping("/bank-accounts/{accountId}")
    public ResponseEntity<?> getBankAccounts(@PathVariable String accountId) {
        if (!callerMayAccessAccount(accountId)) {
            return forbiddenAccountAccess();
        }
        try {
            logger.info("Retrieving bank accounts for Stripe account: {}", accountId);
            List<StripeBankAccountDto> bankAccounts = stripeConnectService.getBankAccounts(accountId);
            return ResponseEntity.ok(bankAccounts);
        } catch (StripeException e) {
            logger.error("Failed to retrieve bank accounts for account: {}. Error: {}", accountId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new StripeConnectResponse("Failed to retrieve bank accounts: " + e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error retrieving bank accounts: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new StripeConnectResponse("Unexpected error: " + e.getMessage()));
        }
    }

    /**
     * Creates a bank account for a Stripe Connect account
     * @param accountId Stripe Connect account ID
     * @param bankAccountToken Token from Stripe.js or Elements
     * @return Created bank account
     */
    @PostMapping(value = "/bank-accounts/{accountId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> createBankAccount(
            @PathVariable String accountId,
            @RequestParam("bankAccountToken") String bankAccountToken) {
        if (!callerMayAccessAccount(accountId)) {
            return forbiddenAccountAccess();
        }
        try {
            logger.info("Creating bank account for Stripe account: {}", accountId);
            StripeBankAccountDto bankAccount = stripeConnectService.createBankAccount(accountId, bankAccountToken);
            return ResponseEntity.ok(bankAccount);
        } catch (StripeException e) {
            logger.error("Failed to create bank account for account: {}. Error: {}", accountId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new StripeConnectResponse("Failed to create bank account: " + e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error creating bank account: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new StripeConnectResponse("Unexpected error: " + e.getMessage()));
        }
    }

    /**
     * Deletes a bank account from a Stripe Connect account
     * @param accountId Stripe Connect account ID
     * @param bankAccountId Bank account ID to delete
     */
    @DeleteMapping("/bank-accounts/{accountId}/{bankAccountId}")
    public ResponseEntity<?> deleteBankAccount(
            @PathVariable String accountId,
            @PathVariable String bankAccountId) {
        if (!callerMayAccessAccount(accountId)) {
            return forbiddenAccountAccess();
        }
        try {
            logger.info("Deleting bank account {} for Stripe account: {}", bankAccountId, accountId);
            stripeConnectService.deleteBankAccount(accountId, bankAccountId);
            return ResponseEntity.ok(Map.of("success", true, "message", "Bank account deleted successfully"));
        } catch (StripeException e) {
            logger.error("Failed to delete bank account {} for account: {}. Error: {}", bankAccountId, accountId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new StripeConnectResponse("Failed to delete bank account: " + e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error deleting bank account: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new StripeConnectResponse("Unexpected error: " + e.getMessage()));
        }
    }

    /**
     * Sets a bank account as default for a currency
     * @param accountId Stripe Connect account ID
     * @param bankAccountId Bank account ID
     * @param currency Currency code (e.g., "usd", "eur")
     * @return Updated bank account
     */
    @PostMapping(value = "/bank-accounts/{accountId}/{bankAccountId}/default", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> setDefaultBankAccount(
            @PathVariable String accountId,
            @PathVariable String bankAccountId,
            @RequestParam("currency") String currency) {
        if (!callerMayAccessAccount(accountId)) {
            return forbiddenAccountAccess();
        }
        try {
            logger.info("Setting bank account {} as default for currency {} in account: {}", bankAccountId, currency, accountId);
            StripeBankAccountDto bankAccount = stripeConnectService.setDefaultBankAccount(accountId, bankAccountId, currency);
            return ResponseEntity.ok(bankAccount);
        } catch (StripeException e) {
            logger.error("Failed to set default bank account {} for account: {}. Error: {}", bankAccountId, accountId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new StripeConnectResponse("Failed to set default bank account: " + e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error setting default bank account: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new StripeConnectResponse("Unexpected error: " + e.getMessage()));
        }
    }

    /**
     * Retrieves transaction history for a Stripe Connect account
     * @param accountId Stripe Connect account ID
     * @param limit Maximum number of transactions to retrieve (default: 10, max: 100)
     * @return List of transactions
     */
    @GetMapping("/transactions/{accountId}")
    public ResponseEntity<?> getTransactions(
            @PathVariable String accountId,
            @RequestParam(value = "limit", required = false) Integer limit) {
        if (!callerMayAccessAccount(accountId)) {
            return forbiddenAccountAccess();
        }
        try {
            logger.info("Retrieving transactions for Stripe account: {} (limit: {})", accountId, limit);
            List<StripeTransactionDto> transactions = stripeConnectService.getTransactions(accountId, limit);
            return ResponseEntity.ok(transactions);
        } catch (StripeException e) {
            logger.error("Failed to retrieve transactions for account: {}. Error: {}", accountId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new StripeConnectResponse("Failed to retrieve transactions: " + e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error retrieving transactions: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new StripeConnectResponse("Unexpected error: " + e.getMessage()));
        }
    }
}


