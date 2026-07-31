package com.stillfresh.app.paymentservice.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.stillfresh.app.paymentservice.model.PaymentUser;
import com.stillfresh.app.paymentservice.repository.PaymentUserRepository;

@Service
public class PaymentUserService {

    @Autowired
    private PaymentUserRepository paymentUserRepository;

	public String getCustomerIdByUsername(String username) {
        return paymentUserRepository.findByUsername(username)
                .map(PaymentUser::getStripeCustomerId)
                .orElse(null);
	}

	/** Returns stored default payment method ID for the user, or null. Used to avoid Customer.retrieve when listing payment methods. */
	public String getDefaultPaymentMethodId(String username) {
        return paymentUserRepository.findByUsername(username)
                .map(PaymentUser::getDefaultPaymentMethodId)
                .orElse(null);
	}

	/** Updates the stored default payment method ID for the user (by username). */
	public void updateDefaultPaymentMethod(String username, String defaultPaymentMethodId) {
        paymentUserRepository.findByUsername(username).ifPresent(pu -> {
            pu.setDefaultPaymentMethodId(defaultPaymentMethodId);
            paymentUserRepository.save(pu);
        });
	}
}

