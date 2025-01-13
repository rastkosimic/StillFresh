package com.stillfresh.app.vendorservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.stillfresh.app.sharedentities.dto.CheckAvailabilityRequest;
import com.stillfresh.app.sharedentities.responses.ApiResponse;
import com.stillfresh.app.vendorservice.config.AuthorizationServiceFeignConfig;

@FeignClient(name = "authorization-service", configuration = AuthorizationServiceFeignConfig.class)
public interface AuthorizationServiceClient {

    @PostMapping(value = "/auth/check-availability", consumes = "application/json")
    ApiResponse checkAvailability(@RequestBody CheckAvailabilityRequest request);
}

