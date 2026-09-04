package com.stillfresh.app.authorizationservice.client;

import java.util.Map;
import com.stillfresh.app.authorizationservice.config.VendorServiceFeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Feign client for communicating with vendor-service
 * Used to fetch vendor details during login
 */
@FeignClient(name = "vendor-service", configuration = VendorServiceFeignConfig.class)
public interface VendorServiceClient {

    /**
     * Get vendor login info by ID (internal service call).
     *
     * <p>Authorized by the shared internal secret, which {@link VendorServiceFeignConfig}
     * attaches to every request.
     */
    @GetMapping("/vendors/internal/{id}/login-info")
    ResponseEntity<Map<String, Object>> getVendorLoginInfo(@PathVariable("id") Long id);
}
