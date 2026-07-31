package com.stillfresh.app.paymentservice.repository;

import com.stillfresh.app.paymentservice.model.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {

    Optional<PaymentTransaction> findByRequestId(String requestId);

    Optional<PaymentTransaction> findByPaymentIntentId(String paymentIntentId);
}
