package com.stillfresh.app.offerservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;

@SpringBootApplication(scanBasePackages = {
	    "com.stillfresh.app.offerservice",
	    "com.stillfresh.app.sharedentities"
	})
@EnableDiscoveryClient
@EnableCaching
@EnableJpaRepositories(basePackages = "com.stillfresh.app.offerservice.repository")
@OpenAPIDefinition(info = @Info(title = "Offer Service API", version = "1.0", description = "Documentation for Offer Service API"))
@EnableFeignClients(basePackages = "com.stillfresh.app.offerservice.client")
public class OfferServiceApplication {

	public static void main(String[] args) {
        SpringApplication.run(OfferServiceApplication.class, args);
	}

}
