package com.stillfresh.app.paymentservice.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.stillfresh.app.paymentservice.model.PaymentUser;

public interface PaymentUserRepository extends JpaRepository<PaymentUser, UUID> {

	Optional<PaymentUser> findByUsername(String username);
	
}
