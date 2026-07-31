package com.stillfresh.app.vendorservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * DTO for setting vendor type (CHAIN or UNIQUE)
 */
public class VendorTypeRequest {
    
    @NotNull(message = "Vendor type is required")
    private VendorType vendorType;
    
    @NotBlank(message = "Chain name is required for CHAIN type")
    private String chainName;  // Required if vendorType is CHAIN
    
    public enum VendorType {
        CHAIN,
        UNIQUE
    }
    
    // Getters and Setters
    
    public VendorType getVendorType() {
        return vendorType;
    }
    
    public void setVendorType(VendorType vendorType) {
        this.vendorType = vendorType;
    }
    
    public String getChainName() {
        return chainName;
    }
    
    public void setChainName(String chainName) {
        this.chainName = chainName;
    }
}

