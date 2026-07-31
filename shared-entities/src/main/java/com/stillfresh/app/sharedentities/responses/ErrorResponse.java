package com.stillfresh.app.sharedentities.responses;

public class ErrorResponse {
    private boolean success;
    private String errorMessage;
    private String code;

    public ErrorResponse(String errorMessage) {
        this.success = false;
        this.errorMessage = errorMessage;
        this.code = null;
    }

    public ErrorResponse(String errorMessage, String code) {
        this.success = false;
        this.errorMessage = errorMessage;
        this.code = code;
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

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}
}
