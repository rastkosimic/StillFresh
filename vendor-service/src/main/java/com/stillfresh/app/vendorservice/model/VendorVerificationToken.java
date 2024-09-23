package com.stillfresh.app.vendorservice.model;

import jakarta.persistence.*;
import java.util.Date;

@Entity
public class VendorVerificationToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String token;

    @OneToOne(targetEntity = Vendor.class, fetch = FetchType.EAGER)
    @JoinColumn(nullable = false, name = "vendor_id")
    private Vendor vendor;

    private Date expiryDate;

    // Getters and Setters

    public Vendor getVendor() {
        return vendor;
    }

    public void setVendor(Vendor vendor) {
        this.vendor = vendor;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Date getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(Date expiryDate) {
        this.expiryDate = expiryDate;
    }
}
