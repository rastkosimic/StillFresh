package com.stillfresh.app.vendorservice.exception;

import com.stillfresh.app.sharedentities.responses.ErrorResponse;
import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import feign.RetryableException;

@ControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class VendorExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(VendorExceptionHandler.class);

    @ExceptionHandler(FeignException.class)
    public ResponseEntity<ErrorResponse> handleFeignException(FeignException ex) {
        HttpStatus status;
        try {
            status = HttpStatus.valueOf(ex.status());
        } catch (Exception e) {
            status = HttpStatus.BAD_GATEWAY;
        }

        // Log status only — never echo downstream bodies (may contain tokens/credentials)
        logger.warn("Downstream call failed: status={}, type={}", ex.status(), ex.getClass().getSimpleName());
        return ResponseEntity.status(status).body(new ErrorResponse("A downstream service request failed."));
    }

    @ExceptionHandler(RetryableException.class)
    public ResponseEntity<ErrorResponse> handleRetryableException(RetryableException ex) {
        logger.warn("Downstream service unavailable: type={}", ex.getClass().getSimpleName());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ErrorResponse("A downstream service is temporarily unavailable."));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException ex) {
        if (ex.getMessage() != null && ex.getMessage().toLowerCase().contains("no instances available")) {
            logger.warn("Service discovery has no instances available");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(new ErrorResponse("A required service is temporarily unavailable."));
        }
        logger.warn("Illegal state in vendor-service: {}", ex.getClass().getSimpleName());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(new ErrorResponse("A downstream gateway error occurred."));
    }
}
