package com.stillfresh.app.paymentservice.repository;

import com.stillfresh.app.paymentservice.model.CustomerPaymentMethod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomerPaymentMethodRepository extends JpaRepository<CustomerPaymentMethod, UUID> {

    List<CustomerPaymentMethod> findByUsernameOrderByCreatedAtDesc(String username);

    Optional<CustomerPaymentMethod> findByReferenceId(String referenceId);

    Optional<CustomerPaymentMethod> findByUsernameAndReferenceId(String username, String referenceId);

    Optional<CustomerPaymentMethod> findFirstByUsernameAndIsDefaultTrue(String username);

    Optional<CustomerPaymentMethod> findFirstByUsernameOrderByCreatedAtDesc(String username);
}
