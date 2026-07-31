package com.stillfresh.app.vendorservice.service.payment;

import com.stillfresh.app.sharedentities.enums.PayoutModel;
import com.stillfresh.app.vendorservice.model.Vendor;
import com.stillfresh.app.vendorservice.model.VendorBalanceTransaction;
import com.stillfresh.app.vendorservice.model.VendorPayout;
import com.stillfresh.app.vendorservice.repository.VendorRepository;
import com.stillfresh.app.vendorservice.repository.VendorBalanceTransactionRepository;
import com.stillfresh.app.vendorservice.repository.VendorPayoutRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Merchant of Record (MoR) implementation of VendorPayoutProcessor
 * Handles internal balance tracking and manual payouts for vendors in unsupported countries
 */
@Service
public class MoRPayoutProcessor implements VendorPayoutProcessor {
    
    private static final Logger logger = LoggerFactory.getLogger(MoRPayoutProcessor.class);
    
    @Autowired
    private VendorRepository vendorRepository;
    
    @Autowired
    private VendorBalanceTransactionRepository balanceTransactionRepository;
    
    @Autowired
    private VendorPayoutRepository payoutRepository;
    
    @Override
    public String registerVendor(String vendorEmail, String vendorName, String country) throws Exception {
        // For MoR, no external registration needed
        // Vendor just needs to provide bank details later
        logger.info("MoR vendor registration: {} ({}) - No external account needed", vendorName, vendorEmail);
        return vendorEmail; // Use email as identifier
    }
    
    @Override
    @Transactional
    public String processPayout(String vendorAccountId, long amount, String currency, String description) throws Exception {
        // This should NOT be called directly for MoR
        // MoR payouts are handled via manual payout requests
        throw new UnsupportedOperationException(
            "MoR payouts are handled manually. Use requestManualPayout() instead.");
    }
    
    /**
     * Adds earnings to vendor's internal balance (called after payment)
     * @param vendorId Vendor ID
     * @param amount Amount in cents
     * @param currency Currency code
     * @param description Description of the transaction
     * @param orderId Order ID if applicable
     */
    @Transactional
    public void addToBalance(Long vendorId, long amount, String currency, String description, Long orderId) {
        Optional<Vendor> vendorOpt = vendorRepository.findById(vendorId);
        if (vendorOpt.isEmpty()) {
            throw new RuntimeException("Vendor not found: " + vendorId);
        }
        
        Vendor vendor = vendorOpt.get();
        
        if (vendor.getPayoutModel() != PayoutModel.MOR) {
            throw new RuntimeException("Vendor is not using MoR model");
        }
        
        BigDecimal amountDecimal = BigDecimal.valueOf(amount);
        BigDecimal currentBalance = vendor.getBalance() != null ? vendor.getBalance() : BigDecimal.ZERO;
        vendor.setBalance(currentBalance.add(amountDecimal));
        vendorRepository.save(vendor);
        
        // Record transaction
        VendorBalanceTransaction transaction = new VendorBalanceTransaction();
        transaction.setVendorId(vendorId);
        transaction.setAmount(amountDecimal);
        transaction.setCurrency(currency);
        transaction.setType("ORDER_PAYMENT");
        transaction.setDescription(description);
        transaction.setOrderId(orderId);
        transaction.setCreatedAt(OffsetDateTime.now());
        balanceTransactionRepository.save(transaction);
        
        logger.info("Added {} {} to vendor {} balance. New balance: {}", 
                   amount, currency, vendorId, vendor.getBalance());
    }
    
    /**
     * @deprecated Vendor payouts are automated via the payment-service ledger
     *             pipeline ({@code /ledger/payouts}). Manual MoR payout requests
     *             are no longer accepted.
     */
    @Deprecated
    @Transactional
    public String requestManualPayout(Long vendorId, long amount, String currency, String description) {
        throw new UnsupportedOperationException(
            "Manual MoR payout requests are deprecated. Vendor payouts are processed "
            + "automatically via the ledger payout pipeline (CMIplus / configured rail). "
            + "Contact platform admin if an exceptional manual payout is required.");
    }

    @Override
    public boolean isAccountReady(String vendorAccountId) throws Exception {
        // For MoR, check if bank details are provided
        Optional<Vendor> vendorOpt = vendorRepository.findByEmail(vendorAccountId);
        
        if (vendorOpt.isEmpty()) {
            return false;
        }
        
        Vendor vendor = vendorOpt.get();
        
        if (vendor.getPayoutModel() != PayoutModel.MOR) {
            return false;
        }
        
        return com.stillfresh.app.vendorservice.service.PaymentRoutingService.hasBankDestination(vendor);
    }
    
    @Override
    public Map<String, Object> getAccountDetails(String vendorAccountId) throws Exception {
        Optional<Vendor> vendorOpt = vendorRepository.findByEmail(vendorAccountId);
        if (vendorOpt.isEmpty()) {
            throw new RuntimeException("Vendor not found");
        }
        
        Vendor vendor = vendorOpt.get();
        
        Map<String, Object> details = new HashMap<>();
        details.put("payoutModel", "MOR");
        details.put("balance", vendor.getBalance() != null ? vendor.getBalance() : BigDecimal.ZERO);
        details.put("manualPayoutMethod", vendor.getManualPayoutMethod());
        details.put("hasBankDetails", com.stillfresh.app.vendorservice.service.PaymentRoutingService.hasBankDestination(vendor));
        // Don't return sensitive bank details
        return details;
    }
    
    @Override
    public String createOnboardingLink(String vendorAccountId) throws Exception {
        // For MoR, return a link to bank details form
        return "/vendors/mor/bank-details"; // Frontend route
    }
    
    @Override
    public String getProviderName() {
        return "MOR";
    }
}

