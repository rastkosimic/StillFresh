package com.stillfresh.app.paymentservice.allsecure;

/**
 * Parsed synchronous response (&lt;result&gt;) from an AllSecure transaction request.
 */
public class AllSecureResult {

    public static final String RETURN_TYPE_FINISHED = "FINISHED";
    public static final String RETURN_TYPE_PENDING = "PENDING";
    public static final String RETURN_TYPE_REDIRECT = "REDIRECT";
    public static final String RETURN_TYPE_ERROR = "ERROR";

    private boolean success;
    private String returnType;
    private String referenceId;
    private String registrationId;
    private String purchaseId;
    private String redirectUrl;
    private String errorMessage;
    private String errorCode;
    private String rawXml;

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getReturnType() { return returnType; }
    public void setReturnType(String returnType) { this.returnType = returnType; }

    public String getReferenceId() { return referenceId; }
    public void setReferenceId(String referenceId) { this.referenceId = referenceId; }

    public String getRegistrationId() { return registrationId; }
    public void setRegistrationId(String registrationId) { this.registrationId = registrationId; }

    public String getPurchaseId() { return purchaseId; }
    public void setPurchaseId(String purchaseId) { this.purchaseId = purchaseId; }

    public String getRedirectUrl() { return redirectUrl; }
    public void setRedirectUrl(String redirectUrl) { this.redirectUrl = redirectUrl; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }

    public String getRawXml() { return rawXml; }
    public void setRawXml(String rawXml) { this.rawXml = rawXml; }

    public boolean isFinished() { return RETURN_TYPE_FINISHED.equalsIgnoreCase(returnType); }
    public boolean isPending() { return RETURN_TYPE_PENDING.equalsIgnoreCase(returnType); }
    public boolean isRedirect() { return RETURN_TYPE_REDIRECT.equalsIgnoreCase(returnType); }
    public boolean isError() { return RETURN_TYPE_ERROR.equalsIgnoreCase(returnType); }

    @Override
    public String toString() {
        return "AllSecureResult{success=" + success + ", returnType='" + returnType + '\''
                + ", referenceId='" + referenceId + '\'' + ", registrationId='" + registrationId + '\''
                + ", redirectUrl='" + redirectUrl + '\'' + ", errorCode='" + errorCode + '\''
                + ", errorMessage='" + errorMessage + '\'' + '}';
    }
}
