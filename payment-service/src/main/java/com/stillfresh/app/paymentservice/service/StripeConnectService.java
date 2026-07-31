package com.stillfresh.app.paymentservice.service;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Account;
import com.stripe.model.AccountLink;
import com.stripe.model.Balance;
import com.stripe.model.BalanceTransaction;
import com.stripe.model.BalanceTransactionCollection;
import com.stripe.model.ExternalAccount;
import com.stripe.model.ExternalAccountCollection;
import com.stripe.model.LoginLink;
import com.stripe.model.Payout;
import com.stripe.model.PayoutCollection;
import com.stripe.net.RequestOptions;
import com.stripe.param.AccountCreateParams;
import com.stripe.param.AccountLinkCreateParams;
import com.stripe.param.BalanceTransactionListParams;
import com.stripe.param.PayoutListParams;
import com.stillfresh.app.paymentservice.dto.StripeAccountDetailsDto;
import com.stillfresh.app.paymentservice.dto.StripeBalanceDto;
import com.stillfresh.app.paymentservice.dto.StripeBankAccountDto;
import com.stillfresh.app.paymentservice.dto.StripePayoutDto;
import com.stillfresh.app.paymentservice.dto.StripeRequirementsDto;
import com.stillfresh.app.paymentservice.dto.StripeTransactionDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class StripeConnectService {

    private static final Logger logger = LoggerFactory.getLogger(StripeConnectService.class);

    @Value("${stripe.apiKey}")
    private String stripeApiKey;

    @Value("${stripe.connect.returnUrl:http://localhost:8083/vendors/stripe/return}")
    private String connectReturnUrl;

    @Value("${stripe.connect.refreshUrl:http://localhost:8083/vendors/stripe/refresh}")
    private String connectRefreshUrl;

    @Value("${stripe.connect.defaultCountry:DE}")
    private String defaultCountry;

    /** TTL for isAccountReady cache: 5 minutes. */
    private static final long ACCOUNT_READY_CACHE_TTL_MS = 5 * 60 * 1000;
    private final Map<String, CachedAccountReady> accountReadyCache = new ConcurrentHashMap<>();

    @Autowired
    public StripeConnectService(@Value("${stripe.apiKey}") String stripeApiKey) {
        // Stripe is already initialized in PaymentService, but ensure it's set here too
        if (Stripe.apiKey == null || Stripe.apiKey.isEmpty()) {
            Stripe.apiKey = stripeApiKey;
        }
    }

    /**
     * Creates a Stripe Connect Express account for a vendor
     * @param vendorEmail Vendor's email address
     * @param vendorName Vendor's business name
     * @return Stripe Connect account ID (e.g., "acct_xxxxx")
     * @throws StripeException if account creation fails (e.g., unsupported country)
     */
    public String createConnectAccount(String vendorEmail, String vendorName) throws StripeException {
        try {
            logger.info("Creating Stripe Connect account for vendor: {} ({}) with default country: {}", 
                       vendorName, vendorEmail, defaultCountry);

            // Create Express account (simpler onboarding for vendors)
            // Note: Country can be changed during onboarding if needed
            AccountCreateParams params = AccountCreateParams.builder()
                    .setType(AccountCreateParams.Type.EXPRESS)
                    .setCountry(defaultCountry) // Use configurable default country
                    .setEmail(vendorEmail)
                    .setCapabilities(
                            AccountCreateParams.Capabilities.builder()
                                    .setCardPayments(AccountCreateParams.Capabilities.CardPayments.builder()
                                            .setRequested(true)
                                            .build())
                                    .setTransfers(AccountCreateParams.Capabilities.Transfers.builder()
                                            .setRequested(true)
                                            .build())
                                    .build()
                    )
                    .build();

            Account account = Account.create(params);
            String accountId = account.getId();
            
            logger.info("Successfully created Stripe Connect account: {} for vendor: {}", accountId, vendorEmail);
            return accountId;
        } catch (StripeException e) {
            // Provide more helpful error message for unsupported countries
            if (e.getCode() != null && e.getCode().equals("country_unsupported")) {
                logger.error("Failed to create Stripe Connect account for vendor: {}. Country '{}' is not supported by Stripe Connect. " +
                           "Please configure a supported country in application.yml (stripe.connect.defaultCountry). " +
                           "Supported countries include: US, CA, GB, DE, FR, AU, and others. Error: {}", 
                           vendorEmail, defaultCountry, e.getMessage());
            } else {
                logger.error("Failed to create Stripe Connect account for vendor: {}. Error: {}", vendorEmail, e.getMessage());
            }
            throw e;
        }
    }

    /**
     * Creates an account link for vendor onboarding
     * Vendor must complete onboarding to receive payments
     * @param accountId Stripe Connect account ID
     * @return URL for vendor to complete onboarding
     */
    public String createAccountLink(String accountId) throws StripeException {
        try {
            logger.info("Creating account link for Stripe Connect account: {}", accountId);

            AccountLinkCreateParams params = AccountLinkCreateParams.builder()
                    .setAccount(accountId)
                    .setRefreshUrl(connectRefreshUrl)
                    .setReturnUrl(connectReturnUrl)
                    .setType(AccountLinkCreateParams.Type.ACCOUNT_ONBOARDING)
                    .build();

            AccountLink accountLink = AccountLink.create(params);
            String onboardingUrl = accountLink.getUrl();

            logger.info("Successfully created account link for account: {}. Onboarding URL: {}", accountId, onboardingUrl);
            return onboardingUrl;
        } catch (StripeException e) {
            logger.error("Failed to create account link for account: {}. Error: {}", accountId, e.getMessage());
            throw e;
        }
    }

    /**
     * Retrieves the onboarding status of a Stripe Connect account (cached for 5 minutes).
     * @param accountId Stripe Connect account ID
     * @return true if account is ready to receive payments, false otherwise
     */
    public boolean isAccountReady(String accountId) throws StripeException {
        CachedAccountReady cached = accountReadyCache.get(accountId);
        if (cached != null && cached.expiresAt > System.currentTimeMillis()) {
            logger.debug("Using cached account ready status for account: {}", accountId);
            return cached.ready;
        }
        if (cached != null) {
            accountReadyCache.remove(accountId);
        }
        try {
            Account account = Account.retrieve(accountId);
            
            // Check if account has completed onboarding
            boolean chargesEnabled = account.getChargesEnabled() != null && account.getChargesEnabled();
            boolean payoutsEnabled = account.getPayoutsEnabled() != null && account.getPayoutsEnabled();
            boolean ready = chargesEnabled && payoutsEnabled;
            
            accountReadyCache.put(accountId, new CachedAccountReady(ready, System.currentTimeMillis() + ACCOUNT_READY_CACHE_TTL_MS));
            logger.info("Account {} status - Charges enabled: {}, Payouts enabled: {}", 
                    accountId, chargesEnabled, payoutsEnabled);
            
            return ready;
        } catch (StripeException e) {
            logger.error("Failed to retrieve account status for: {}. Error: {}", accountId, e.getMessage());
            throw e;
        }
    }

    private static final class CachedAccountReady {
        final boolean ready;
        final long expiresAt;

        CachedAccountReady(boolean ready, long expiresAt) {
            this.ready = ready;
            this.expiresAt = expiresAt;
        }
    }

    /**
     * Retrieves a Stripe Connect account by ID
     * @param accountId Stripe Connect account ID
     * @return Stripe Account object
     */
    public Account getAccount(String accountId) throws StripeException {
        try {
            return Account.retrieve(accountId);
        } catch (StripeException e) {
            logger.error("Failed to retrieve account: {}. Error: {}", accountId, e.getMessage());
            throw e;
        }
    }

    /**
     * Retrieves detailed account information for a Stripe Connect account
     * @param accountId Stripe Connect account ID
     * @return Account details DTO
     */
    public StripeAccountDetailsDto getAccountDetails(String accountId) throws StripeException {
        try {
            Account account = Account.retrieve(accountId);
            StripeAccountDetailsDto dto = new StripeAccountDetailsDto();
            
            dto.setAccountId(account.getId());
            dto.setEmail(account.getEmail());
            dto.setCountry(account.getCountry());
            dto.setDefaultCurrency(account.getDefaultCurrency());
            dto.setType(account.getType() != null ? account.getType().toString() : null);
            dto.setChargesEnabled(account.getChargesEnabled());
            dto.setPayoutsEnabled(account.getPayoutsEnabled());
            dto.setDetailsSubmitted(account.getDetailsSubmitted());
            dto.setBusinessType(account.getBusinessType() != null ? account.getBusinessType().toString() : null);
            
            if (account.getBusinessProfile() != null) {
                dto.setBusinessProfileName(account.getBusinessProfile().getName());
            }
            
            // Convert capabilities to map - simplified approach
            // Capabilities are complex objects, we'll set a basic map for now
            // Can be enhanced later with proper capability parsing
            Map<String, Object> capabilities = new HashMap<>();
            if (account.getCapabilities() != null) {
                capabilities.put("hasCapabilities", true);
            }
            dto.setCapabilities(capabilities);
            
            // Convert requirements to map
            if (account.getRequirements() != null) {
                Map<String, Object> requirements = new HashMap<>();
                if (account.getRequirements().getCurrentlyDue() != null) {
                    requirements.put("currentlyDue", account.getRequirements().getCurrentlyDue().stream()
                        .map(Object::toString).collect(Collectors.toList()));
                }
                if (account.getRequirements().getPastDue() != null) {
                    requirements.put("pastDue", account.getRequirements().getPastDue().stream()
                        .map(Object::toString).collect(Collectors.toList()));
                }
                if (account.getRequirements().getPendingVerification() != null) {
                    requirements.put("pendingVerification", account.getRequirements().getPendingVerification().stream()
                        .map(Object::toString).collect(Collectors.toList()));
                }
                if (account.getRequirements().getDisabledReason() != null) {
                    requirements.put("disabledReason", account.getRequirements().getDisabledReason());
                }
                dto.setRequirements(requirements);
            }
            
            logger.info("Retrieved account details for account: {}", accountId);
            return dto;
        } catch (StripeException e) {
            logger.error("Failed to retrieve account details for: {}. Error: {}", accountId, e.getMessage());
            throw e;
        }
    }

    /**
     * Creates a login link for the Stripe Express Dashboard
     * @param accountId Stripe Connect account ID
     * @return Login URL for the Stripe dashboard
     */
    public String createLoginLink(String accountId) throws StripeException {
        try {
            logger.info("Creating login link for Stripe Connect account: {}", accountId);
            
            Map<String, Object> params = new HashMap<>();
            
            LoginLink loginLink = LoginLink.createOnAccount(accountId, params, 
                    RequestOptions.builder().build());
            String loginUrl = loginLink.getUrl();
            
            logger.info("Successfully created login link for account: {}. Login URL: {}", accountId, loginUrl);
            return loginUrl;
        } catch (StripeException e) {
            logger.error("Failed to create login link for account: {}. Error: {}", accountId, e.getMessage());
            throw e;
        }
    }

    /**
     * Retrieves payouts for a Stripe Connect account
     * @param accountId Stripe Connect account ID
     * @param limit Maximum number of payouts to retrieve (default: 10, max: 100)
     * @return List of payout DTOs
     */
    public List<StripePayoutDto> getPayouts(String accountId, Integer limit) throws StripeException {
        try {
            logger.info("Retrieving payouts for Stripe Connect account: {} (limit: {})", accountId, limit);
            
            PayoutListParams.Builder paramsBuilder = PayoutListParams.builder()
                    .setLimit(limit != null ? Long.valueOf(Math.min(limit, 100)) : 10L);
            
            PayoutCollection payouts = Payout.list(paramsBuilder.build(), 
                    RequestOptions.builder().setStripeAccount(accountId).build());
            
            List<StripePayoutDto> payoutDtos = new ArrayList<>();
            for (Payout payout : payouts.getData()) {
                StripePayoutDto dto = new StripePayoutDto();
                dto.setPayoutId(payout.getId());
                dto.setAmount(payout.getAmount());
                dto.setCurrency(payout.getCurrency());
                dto.setStatus(payout.getStatus() != null ? payout.getStatus().toString() : null);
                dto.setArrivalDate(payout.getArrivalDate() != null ? 
                    java.time.Instant.ofEpochSecond(payout.getArrivalDate()) : null);
                dto.setCreated(payout.getCreated() != null ? 
                    java.time.Instant.ofEpochSecond(payout.getCreated()) : null);
                dto.setDescription(payout.getDescription());
                dto.setDestination(payout.getDestination());
                dto.setFailureCode(payout.getFailureCode());
                dto.setFailureMessage(payout.getFailureMessage());
                dto.setMethod(payout.getMethod() != null ? payout.getMethod().toString() : null);
                dto.setStatementDescriptor(payout.getStatementDescriptor());
                payoutDtos.add(dto);
            }
            
            logger.info("Retrieved {} payouts for account: {}", payoutDtos.size(), accountId);
            return payoutDtos;
        } catch (StripeException e) {
            logger.error("Failed to retrieve payouts for account: {}. Error: {}", accountId, e.getMessage());
            throw e;
        }
    }

    /**
     * Retrieves a specific payout by ID
     * @param accountId Stripe Connect account ID
     * @param payoutId Payout ID
     * @return Payout DTO
     */
    public StripePayoutDto getPayout(String accountId, String payoutId) throws StripeException {
        try {
            logger.info("Retrieving payout {} for Stripe Connect account: {}", payoutId, accountId);
            
            Payout payout = Payout.retrieve(payoutId, 
                    RequestOptions.builder().setStripeAccount(accountId).build());
            
            StripePayoutDto dto = new StripePayoutDto();
            dto.setPayoutId(payout.getId());
            dto.setAmount(payout.getAmount());
            dto.setCurrency(payout.getCurrency());
            dto.setStatus(payout.getStatus() != null ? payout.getStatus().toString() : null);
            if (payout.getArrivalDate() != null) {
                dto.setArrivalDate(java.time.Instant.ofEpochSecond(payout.getArrivalDate()));
            }
            if (payout.getCreated() != null) {
                dto.setCreated(java.time.Instant.ofEpochSecond(payout.getCreated()));
            }
            dto.setDescription(payout.getDescription());
            dto.setDestination(payout.getDestination());
            dto.setFailureCode(payout.getFailureCode());
            dto.setFailureMessage(payout.getFailureMessage());
            dto.setMethod(payout.getMethod() != null ? payout.getMethod().toString() : null);
            dto.setStatementDescriptor(payout.getStatementDescriptor());
            
            logger.info("Retrieved payout {} for account: {}", payoutId, accountId);
            return dto;
        } catch (StripeException e) {
            logger.error("Failed to retrieve payout {} for account: {}. Error: {}", payoutId, accountId, e.getMessage());
            throw e;
        }
    }

    /**
     * Retrieves the balance for a Stripe Connect account
     * @param accountId Stripe Connect account ID
     * @return Balance DTO
     */
    public StripeBalanceDto getBalance(String accountId) throws StripeException {
        try {
            logger.info("Retrieving balance for Stripe Connect account: {}", accountId);
            
            Balance balance = Balance.retrieve(
                    RequestOptions.builder().setStripeAccount(accountId).build());
            
            StripeBalanceDto dto = new StripeBalanceDto();
            
            // Convert available balance
            if (balance.getAvailable() != null) {
                dto.setAvailable(balance.getAvailable().stream()
                    .map(b -> {
                        StripeBalanceDto.BalanceAmount amount = new StripeBalanceDto.BalanceAmount();
                        amount.setAmount(b.getAmount());
                        amount.setCurrency(b.getCurrency());
                        // Source types are stored as a map in Stripe, but we'll simplify for now
                        // Leave sources empty for now - can be enhanced later if needed
                        amount.setSources(new ArrayList<>());
                        return amount;
                    })
                    .collect(Collectors.toList()));
            }
            
            // Convert pending balance
            if (balance.getPending() != null) {
                dto.setPending(balance.getPending().stream()
                    .map(b -> {
                        StripeBalanceDto.BalanceAmount amount = new StripeBalanceDto.BalanceAmount();
                        amount.setAmount(b.getAmount());
                        amount.setCurrency(b.getCurrency());
                        // Source types are stored as a map in Stripe, but we'll simplify for now
                        // Leave sources empty for now - can be enhanced later if needed
                        amount.setSources(new ArrayList<>());
                        return amount;
                    })
                    .collect(Collectors.toList()));
            }
            
            // Convert instant available balance (if available)
            if (balance.getInstantAvailable() != null) {
                dto.setInstantAvailable(balance.getInstantAvailable().stream()
                    .map(b -> {
                        StripeBalanceDto.BalanceAmount amount = new StripeBalanceDto.BalanceAmount();
                        amount.setAmount(b.getAmount());
                        amount.setCurrency(b.getCurrency());
                        return amount;
                    })
                    .collect(Collectors.toList()));
            }
            
            logger.info("Retrieved balance for account: {}", accountId);
            return dto;
        } catch (StripeException e) {
            logger.error("Failed to retrieve balance for account: {}. Error: {}", accountId, e.getMessage());
            throw e;
        }
    }

    /**
     * Retrieves verification requirements for a Stripe Connect account
     * @param accountId Stripe Connect account ID
     * @return Requirements DTO
     */
    public StripeRequirementsDto getRequirements(String accountId) throws StripeException {
        try {
            logger.info("Retrieving requirements for Stripe Connect account: {}", accountId);
            
            Account account = Account.retrieve(accountId);
            StripeRequirementsDto dto = new StripeRequirementsDto();
            
            if (account.getRequirements() != null) {
                if (account.getRequirements().getCurrentlyDue() != null) {
                    dto.setCurrentlyDue(account.getRequirements().getCurrentlyDue().stream()
                        .map(Object::toString).collect(Collectors.toList()));
                }
                if (account.getRequirements().getEventuallyDue() != null) {
                    dto.setEventuallyDue(account.getRequirements().getEventuallyDue().stream()
                        .map(Object::toString).collect(Collectors.toList()));
                }
                if (account.getRequirements().getPastDue() != null) {
                    dto.setPastDue(account.getRequirements().getPastDue().stream()
                        .map(Object::toString).collect(Collectors.toList()));
                }
                if (account.getRequirements().getPendingVerification() != null) {
                    dto.setPendingVerification(account.getRequirements().getPendingVerification().stream()
                        .map(Object::toString).collect(Collectors.toList()));
                }
                dto.setDisabledReason(account.getRequirements().getDisabledReason());
                dto.setCurrentDeadline(account.getRequirements().getCurrentDeadline());
                // eventuallyDeadline is not available in the SDK
            }
            
            logger.info("Retrieved requirements for account: {}", accountId);
            return dto;
        } catch (StripeException e) {
            logger.error("Failed to retrieve requirements for account: {}. Error: {}", accountId, e.getMessage());
            throw e;
        }
    }

    /**
     * Lists all bank accounts for a Stripe Connect account
     * @param accountId Stripe Connect account ID
     * @return List of bank account DTOs
     */
    public List<StripeBankAccountDto> getBankAccounts(String accountId) throws StripeException {
        try {
            logger.info("Retrieving bank accounts for Stripe Connect account: {}", accountId);
            
            Map<String, Object> params = new HashMap<>();
            params.put("limit", 100);
            
            ExternalAccountCollection externalAccounts = Account.retrieve(accountId)
                    .getExternalAccounts()
                    .list(params, RequestOptions.builder().setStripeAccount(accountId).build());
            
            List<StripeBankAccountDto> bankAccounts = new ArrayList<>();
            for (ExternalAccount externalAccount : externalAccounts.getData()) {
                if (externalAccount instanceof com.stripe.model.BankAccount) {
                    com.stripe.model.BankAccount bankAccount = (com.stripe.model.BankAccount) externalAccount;
                    StripeBankAccountDto dto = new StripeBankAccountDto();
                    dto.setBankAccountId(bankAccount.getId());
                    dto.setAccountHolderName(bankAccount.getAccountHolderName());
                    dto.setAccountHolderType(bankAccount.getAccountHolderType() != null ? 
                        bankAccount.getAccountHolderType().toString() : null);
                    dto.setBankName(bankAccount.getBankName());
                    dto.setCountry(bankAccount.getCountry());
                    dto.setCurrency(bankAccount.getCurrency());
                    dto.setLast4(bankAccount.getLast4());
                    dto.setRoutingNumber(bankAccount.getRoutingNumber());
                    dto.setStatus(bankAccount.getStatus() != null ? bankAccount.getStatus().toString() : null);
                    dto.setDefaultForCurrency(bankAccount.getDefaultForCurrency());
                    dto.setFingerprint(bankAccount.getFingerprint());
                    bankAccounts.add(dto);
                }
            }
            
            logger.info("Retrieved {} bank accounts for account: {}", bankAccounts.size(), accountId);
            return bankAccounts;
        } catch (StripeException e) {
            logger.error("Failed to retrieve bank accounts for account: {}. Error: {}", accountId, e.getMessage());
            throw e;
        }
    }

    /**
     * Creates a bank account for a Stripe Connect account using bank account token
     * @param accountId Stripe Connect account ID
     * @param bankAccountToken Token from Stripe.js or Elements for the bank account
     * @return Created bank account DTO
     */
    public StripeBankAccountDto createBankAccount(String accountId, String bankAccountToken) throws StripeException {
        try {
            logger.info("Creating bank account for Stripe Connect account: {}", accountId);
            
            Map<String, Object> params = new HashMap<>();
            params.put("external_account", bankAccountToken);
            
            ExternalAccount externalAccount = Account.retrieve(accountId)
                    .getExternalAccounts()
                    .create(params, RequestOptions.builder().setStripeAccount(accountId).build());
            
            if (externalAccount instanceof com.stripe.model.BankAccount) {
                com.stripe.model.BankAccount bankAccount = (com.stripe.model.BankAccount) externalAccount;
                StripeBankAccountDto dto = new StripeBankAccountDto();
                dto.setBankAccountId(bankAccount.getId());
                dto.setAccountHolderName(bankAccount.getAccountHolderName());
                dto.setAccountHolderType(bankAccount.getAccountHolderType() != null ? 
                    bankAccount.getAccountHolderType().toString() : null);
                dto.setBankName(bankAccount.getBankName());
                dto.setCountry(bankAccount.getCountry());
                dto.setCurrency(bankAccount.getCurrency());
                dto.setLast4(bankAccount.getLast4());
                dto.setRoutingNumber(bankAccount.getRoutingNumber());
                dto.setStatus(bankAccount.getStatus() != null ? bankAccount.getStatus().toString() : null);
                dto.setDefaultForCurrency(bankAccount.getDefaultForCurrency());
                dto.setFingerprint(bankAccount.getFingerprint());
                
                logger.info("Created bank account {} for account: {}", bankAccount.getId(), accountId);
                return dto;
            } else {
                throw new RuntimeException("Created external account is not a bank account");
            }
        } catch (StripeException e) {
            logger.error("Failed to create bank account for account: {}. Error: {}", accountId, e.getMessage());
            throw e;
        }
    }

    /**
     * Deletes a bank account from a Stripe Connect account
     * @param accountId Stripe Connect account ID
     * @param bankAccountId Bank account ID to delete
     */
    public void deleteBankAccount(String accountId, String bankAccountId) throws StripeException {
        try {
            logger.info("Deleting bank account {} for Stripe Connect account: {}", bankAccountId, accountId);
            
            Account.retrieve(accountId)
                    .getExternalAccounts()
                    .retrieve(bankAccountId, RequestOptions.builder().setStripeAccount(accountId).build())
                    .delete(RequestOptions.builder().setStripeAccount(accountId).build());
            
            logger.info("Deleted bank account {} for account: {}", bankAccountId, accountId);
        } catch (StripeException e) {
            logger.error("Failed to delete bank account {} for account: {}. Error: {}", bankAccountId, accountId, e.getMessage());
            throw e;
        }
    }

    /**
     * Sets a bank account as default for a specific currency
     * @param accountId Stripe Connect account ID
     * @param bankAccountId Bank account ID to set as default
     * @param currency Currency code (e.g., "usd", "eur")
     * @return Updated bank account DTO
     */
    public StripeBankAccountDto setDefaultBankAccount(String accountId, String bankAccountId, String currency) throws StripeException {
        try {
            logger.info("Setting bank account {} as default for currency {} in account: {}", bankAccountId, currency, accountId);
            
            // Update the external account to set it as default for the currency
            Map<String, Object> updateParams = new HashMap<>();
            updateParams.put("default_for_currency", currency);
            
            ExternalAccount externalAccount = Account.retrieve(accountId)
                    .getExternalAccounts()
                    .retrieve(bankAccountId, RequestOptions.builder().setStripeAccount(accountId).build());
            
            // Use the update method if available, otherwise use direct API call
            com.stripe.model.BankAccount bankAccount = (com.stripe.model.BankAccount) externalAccount;
            com.stripe.model.BankAccount updatedBankAccount = bankAccount.update(updateParams, 
                    RequestOptions.builder().setStripeAccount(accountId).build());
            
            StripeBankAccountDto dto = new StripeBankAccountDto();
            dto.setBankAccountId(updatedBankAccount.getId());
            dto.setAccountHolderName(updatedBankAccount.getAccountHolderName());
            dto.setAccountHolderType(updatedBankAccount.getAccountHolderType() != null ? 
                updatedBankAccount.getAccountHolderType().toString() : null);
            dto.setBankName(updatedBankAccount.getBankName());
            dto.setCountry(updatedBankAccount.getCountry());
            dto.setCurrency(updatedBankAccount.getCurrency());
            dto.setLast4(updatedBankAccount.getLast4());
            dto.setRoutingNumber(updatedBankAccount.getRoutingNumber());
            dto.setStatus(updatedBankAccount.getStatus() != null ? updatedBankAccount.getStatus().toString() : null);
            dto.setDefaultForCurrency(updatedBankAccount.getDefaultForCurrency());
            dto.setFingerprint(updatedBankAccount.getFingerprint());
            
            logger.info("Set bank account {} as default for currency {} in account: {}", bankAccountId, currency, accountId);
            return dto;
        } catch (StripeException e) {
            logger.error("Failed to set default bank account {} for account: {}. Error: {}", bankAccountId, accountId, e.getMessage());
            throw e;
        }
    }

    /**
     * Retrieves transaction history for a Stripe Connect account
     * @param accountId Stripe Connect account ID
     * @param limit Maximum number of transactions to retrieve (default: 10, max: 100)
     * @return List of transaction DTOs
     */
    public List<StripeTransactionDto> getTransactions(String accountId, Integer limit) throws StripeException {
        try {
            logger.info("Retrieving transactions for Stripe Connect account: {} (limit: {})", accountId, limit);
            
            BalanceTransactionListParams.Builder paramsBuilder = BalanceTransactionListParams.builder()
                    .setLimit(limit != null ? Long.valueOf(Math.min(limit, 100)) : 10L);
            
            BalanceTransactionCollection transactions = BalanceTransaction.list(
                    paramsBuilder.build(),
                    RequestOptions.builder().setStripeAccount(accountId).build());
            
            List<StripeTransactionDto> transactionDtos = new ArrayList<>();
            for (BalanceTransaction transaction : transactions.getData()) {
                StripeTransactionDto dto = new StripeTransactionDto();
                dto.setTransactionId(transaction.getId());
                dto.setAmount(transaction.getAmount());
                dto.setCurrency(transaction.getCurrency());
                dto.setDescription(transaction.getDescription());
                if (transaction.getCreated() != null) {
                    dto.setCreated(java.time.Instant.ofEpochSecond(transaction.getCreated()));
                }
                dto.setType(transaction.getType());
                dto.setStatus(transaction.getStatus() != null ? transaction.getStatus().toString() : null);
                dto.setFee(transaction.getFee());
                dto.setNet(transaction.getNet());
                dto.setSource(transaction.getSource());
                dto.setReportingCategory(transaction.getReportingCategory());
                transactionDtos.add(dto);
            }
            
            logger.info("Retrieved {} transactions for account: {}", transactionDtos.size(), accountId);
            return transactionDtos;
        } catch (StripeException e) {
            logger.error("Failed to retrieve transactions for account: {}. Error: {}", accountId, e.getMessage());
            throw e;
        }
    }
}


