package com.stillfresh.app.paymentservice.dto;

import java.io.Serializable;
import java.util.List;

public class StripeBalanceDto implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private List<BalanceAmount> available;
    private List<BalanceAmount> pending;
    private List<BalanceAmount> instantAvailable;
    
    public StripeBalanceDto() {
    }
    
    public List<BalanceAmount> getAvailable() {
        return available;
    }
    
    public void setAvailable(List<BalanceAmount> available) {
        this.available = available;
    }
    
    public List<BalanceAmount> getPending() {
        return pending;
    }
    
    public void setPending(List<BalanceAmount> pending) {
        this.pending = pending;
    }
    
    public List<BalanceAmount> getInstantAvailable() {
        return instantAvailable;
    }
    
    public void setInstantAvailable(List<BalanceAmount> instantAvailable) {
        this.instantAvailable = instantAvailable;
    }
    
    public static class BalanceAmount implements Serializable {
        private static final long serialVersionUID = 1L;
        
        private Long amount; // Amount in cents
        private String currency;
        private List<Source> sources;
        
        public BalanceAmount() {
        }
        
        public Long getAmount() {
            return amount;
        }
        
        public void setAmount(Long amount) {
            this.amount = amount;
        }
        
        public String getCurrency() {
            return currency;
        }
        
        public void setCurrency(String currency) {
            this.currency = currency;
        }
        
        public List<Source> getSources() {
            return sources;
        }
        
        public void setSources(List<Source> sources) {
            this.sources = sources;
        }
    }
    
    public static class Source implements Serializable {
        private static final long serialVersionUID = 1L;
        
        private String id;
        private String type; // card, bank_account, etc.
        
        public Source() {
        }
        
        public String getId() {
            return id;
        }
        
        public void setId(String id) {
            this.id = id;
        }
        
        public String getType() {
            return type;
        }
        
        public void setType(String type) {
            this.type = type;
        }
    }
}

