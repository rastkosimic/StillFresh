package com.stillfresh.app.paymentservice.dto;

import java.io.Serializable;
import java.util.List;

public class StripeRequirementsDto implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private List<String> currentlyDue;
    private List<String> eventuallyDue;
    private List<String> pastDue;
    private List<String> pendingVerification;
    private String disabledReason;
    private Long currentDeadline;
    private Long eventuallyDeadline;
    
    public StripeRequirementsDto() {
    }
    
    public List<String> getCurrentlyDue() {
        return currentlyDue;
    }
    
    public void setCurrentlyDue(List<String> currentlyDue) {
        this.currentlyDue = currentlyDue;
    }
    
    public List<String> getEventuallyDue() {
        return eventuallyDue;
    }
    
    public void setEventuallyDue(List<String> eventuallyDue) {
        this.eventuallyDue = eventuallyDue;
    }
    
    public List<String> getPastDue() {
        return pastDue;
    }
    
    public void setPastDue(List<String> pastDue) {
        this.pastDue = pastDue;
    }
    
    public List<String> getPendingVerification() {
        return pendingVerification;
    }
    
    public void setPendingVerification(List<String> pendingVerification) {
        this.pendingVerification = pendingVerification;
    }
    
    public String getDisabledReason() {
        return disabledReason;
    }
    
    public void setDisabledReason(String disabledReason) {
        this.disabledReason = disabledReason;
    }
    
    public Long getCurrentDeadline() {
        return currentDeadline;
    }
    
    public void setCurrentDeadline(Long currentDeadline) {
        this.currentDeadline = currentDeadline;
    }
    
    public Long getEventuallyDeadline() {
        return eventuallyDeadline;
    }
    
    public void setEventuallyDeadline(Long eventuallyDeadline) {
        this.eventuallyDeadline = eventuallyDeadline;
    }
}

