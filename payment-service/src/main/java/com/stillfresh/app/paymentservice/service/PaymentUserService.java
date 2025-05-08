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
}

