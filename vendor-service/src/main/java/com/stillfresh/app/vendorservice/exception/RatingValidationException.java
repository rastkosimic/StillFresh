package com.stillfresh.app.vendorservice.exception;

import org.springframework.http.HttpStatus;

public class RatingValidationException extends RuntimeException {

    private final HttpStatus status;

    public RatingValidationException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
