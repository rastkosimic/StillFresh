package com.stillfresh.app.paymentservice.model;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * A customer's stored payment instrument registered with AllSecure.
 *
 * <p>AllSecure does not expose a "list stored cards" API, so card metadata is persisted locally at
 * registration time. The {@code referenceId} is the AllSecure registration UUID used as
 * {@code referenceTransactionId} for subsequent card-on-file charges.</p>
 */
@Entity
@Table(name = "customer_payment_methods",
       indexes = {
           @Index(name = "idx_cpm_username", columnList = "username"),
           @Index(name = "idx_cpm_reference_id", columnList = "reference_id", unique = true)
       })
public class CustomerPaymentMethod {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false)
    private String username;

    /** AllSecure registration referenceId (UUID); used as referenceTransactionId for card-on-file charges. */
    @Column(name = "reference_id", nullable = false, unique = true, length = 255)
    private String referenceId;

    /** AllSecure registrationId, if returned. */
    @Column(name = "registration_id", length = 255)
    private String registrationId;

    @Column(name = "brand", length = 64)
    private String brand;

    @Column(name = "last4", length = 8)
    private String last4;

    @Column(name = "exp_month", length = 4)
    private String expMonth;

    @Column(name = "exp_year", length = 8)
    private String expYear;

    @Column(name = "is_default", nullable = false)
    private boolean isDefault = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public CustomerPaymentMethod() {}

    public UUID getId() { return id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getReferenceId() { return referenceId; }
    public void setReferenceId(String referenceId) { this.referenceId = referenceId; }

    public String getRegistrationId() { return registrationId; }
    public void setRegistrationId(String registrationId) { this.registrationId = registrationId; }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }

    public String getLast4() { return last4; }
    public void setLast4(String last4) { this.last4 = last4; }

    public String getExpMonth() { return expMonth; }
    public void setExpMonth(String expMonth) { this.expMonth = expMonth; }

    public String getExpYear() { return expYear; }
    public void setExpYear(String expYear) { this.expYear = expYear; }

    public boolean isDefault() { return isDefault; }
    public void setDefault(boolean aDefault) { isDefault = aDefault; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
