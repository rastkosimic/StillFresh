package com.stillfresh.app.sharedentities.payment.events;

import java.util.List;
import java.util.Map;

/**
 * Event sent from vendor-service to payment-service with MoR payout data
 */
public class MoRPayoutDataResponseEvent {
    private String requestId;
    private String requestType;
    private boolean success;
    private String errorMessage;
    private List<Map<String, Object>> data; // For list responses
    private Map<String, Object> summary; // For summary responses

    public MoRPayoutDataResponseEvent() {}

    public MoRPayoutDataResponseEvent(String requestId, String requestType, boolean success) {
        this.requestId = requestId;
        this.requestType = requestType;
        this.success = success;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getRequestType() {
        return requestType;
    }

    public void setRequestType(String requestType) {
        this.requestType = requestType;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public List<Map<String, Object>> getData() {
        return data;
    }

    public void setData(List<Map<String, Object>> data) {
        this.data = data;
    }

    public Map<String, Object> getSummary() {
        return summary;
    }

    public void setSummary(Map<String, Object> summary) {
        this.summary = summary;
    }
}

