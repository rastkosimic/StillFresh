package com.stillfresh.app.vendorservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;

@SpringBootApplication(scanBasePackages = {
	    "com.stillfresh.app.vendorservice",
	    "com.stillfresh.app.sharedentities"
	})
@EnableDiscoveryClient
@EnableCaching
@EnableJpaRepositories(basePackages = "com.stillfresh.app.vendorservice.repository")
@OpenAPIDefinition(info = @Info(title = "Vendor Service API", version = "1.0", description = "Documentation for Vendor Service API"))
@EnableFeignClients(basePackages = "com.stillfresh.app.vendorservice.client")
public class VendorServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(VendorServiceApplication.class, args);
    }
}