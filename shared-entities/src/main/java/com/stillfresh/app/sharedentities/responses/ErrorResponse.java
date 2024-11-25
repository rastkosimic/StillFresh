package com.stillfresh.app.sharedentities.responses;

public class ErrorResponse {
    private boolean success;
    private String errorMessage;

    public ErrorResponse(String errorMessage) {
        this.success = false;
        this.errorMessage = errorMessage;
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
}
