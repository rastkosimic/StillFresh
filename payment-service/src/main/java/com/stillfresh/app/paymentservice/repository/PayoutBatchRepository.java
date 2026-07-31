package com.stillfresh.app.paymentservice.repository;

import com.stillfresh.app.paymentservice.model.PayoutBatch;
import com.stillfresh.app.sharedentities.enums.PayoutStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PayoutBatchRepository extends JpaRepository<PayoutBatch, Long> {

    Page<PayoutBatch> findAllByOrderByScheduledAtDesc(Pageable pageable);

    List<PayoutBatch> findByStatus(PayoutStatus status);
}
