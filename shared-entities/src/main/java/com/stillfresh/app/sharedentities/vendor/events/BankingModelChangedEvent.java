package com.stillfresh.app.sharedentities.vendor.events;

/**
 * Event published when banking model is switched for a chain
 * Notifies all chain locations about the change
 */
public class BankingModelChangedEvent {
    private String chainId;
    private String chainName;
    private String newBankingModel;  // "SHARED" or "INDIVIDUAL"
    private String previousBankingModel;  // "SHARED" or "INDIVIDUAL"
    private Long changedByVendorId;  // ID of the VENDOR_ADMIN who made the change
    private String changedByEmail;  // Email of the VENDOR_ADMIN who made the change
    private Long headquartersVendorId;  // HQ vendor ID (for SHARED model)
    private String headquartersEmail;  // HQ email (for SHARED model)
    private java.util.List<Long> locationVendorIds;  // All location vendor IDs to notify
    private java.time.OffsetDateTime changedAt;

    public BankingModelChangedEvent() {
        this.changedAt = java.time.OffsetDateTime.now();
    }

    public BankingModelChangedEvent(String chainId, String chainName, String newBankingModel, 
                                   String previousBankingModel, Long changedByVendorId, String changedByEmail,
                                   Long headquartersVendorId, String headquartersEmail, 
                                   java.util.List<Long> locationVendorIds) {
        this.chainId = chainId;
        this.chainName = chainName;
        this.newBankingModel = newBankingModel;
        this.previousBankingModel = previousBankingModel;
        this.changedByVendorId = changedByVendorId;
        this.changedByEmail = changedByEmail;
        this.headquartersVendorId = headquartersVendorId;
        this.headquartersEmail = headquartersEmail;
        this.locationVendorIds = locationVendorIds;
        this.changedAt = java.time.OffsetDateTime.now();
    }

    // Getters and Setters
    public String getChainId() {
        return chainId;
    }

    public void setChainId(String chainId) {
        this.chainId = chainId;
    }

    public String getChainName() {
        return chainName;
    }

    public void setChainName(String chainName) {
        this.chainName = chainName;
    }

    public String getNewBankingModel() {
        return newBankingModel;
    }

    public void setNewBankingModel(String newBankingModel) {
        this.newBankingModel = newBankingModel;
    }

    public String getPreviousBankingModel() {
        return previousBankingModel;
    }

    public void setPreviousBankingModel(String previousBankingModel) {
        this.previousBankingModel = previousBankingModel;
    }

    public Long getChangedByVendorId() {
        return changedByVendorId;
    }

    public void setChangedByVendorId(Long changedByVendorId) {
        this.changedByVendorId = changedByVendorId;
    }

    public String getChangedByEmail() {
        return changedByEmail;
    }

    public void setChangedByEmail(String changedByEmail) {
        this.changedByEmail = changedByEmail;
    }

    public Long getHeadquartersVendorId() {
        return headquartersVendorId;
    }

    public void setHeadquartersVendorId(Long headquartersVendorId) {
        this.headquartersVendorId = headquartersVendorId;
    }

    public String getHeadquartersEmail() {
        return headquartersEmail;
    }

    public void setHeadquartersEmail(String headquartersEmail) {
        this.headquartersEmail = headquartersEmail;
    }

    public java.time.OffsetDateTime getChangedAt() {
        return changedAt;
    }

    public void setChangedAt(java.time.OffsetDateTime changedAt) {
        this.changedAt = changedAt;
    }

    public java.util.List<Long> getLocationVendorIds() {
        return locationVendorIds;
    }

    public void setLocationVendorIds(java.util.List<Long> locationVendorIds) {
        this.locationVendorIds = locationVendorIds;
    }

    @Override
    public String toString() {
        return "BankingModelChangedEvent{" +
                "chainId='" + chainId + '\'' +
                ", chainName='" + chainName + '\'' +
                ", newBankingModel='" + newBankingModel + '\'' +
                ", previousBankingModel='" + previousBankingModel + '\'' +
                ", changedByVendorId=" + changedByVendorId +
                ", changedByEmail='" + changedByEmail + '\'' +
                ", headquartersVendorId=" + headquartersVendorId +
                ", headquartersEmail='" + headquartersEmail + '\'' +
                ", changedAt=" + changedAt +
                '}';
    }
}

