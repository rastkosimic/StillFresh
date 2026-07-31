package com.stillfresh.app.paymentservice.repository;

import com.stillfresh.app.paymentservice.model.BankTransferPayment;
import com.stillfresh.app.paymentservice.model.BankTransferStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BankTransferPaymentRepository extends JpaRepository<BankTransferPayment, Long> {

    Optional<BankTransferPayment> findByPaymentReference(String paymentReference);

    Optional<BankTransferPayment> findByOrderId(Long orderId);

    Page<BankTransferPayment> findByStatusOrderByCreatedAtDesc(BankTransferStatus status, Pageable pageable);

    List<BankTransferPayment> findByUserIdOrderByCreatedAtDesc(Long userId);
}
