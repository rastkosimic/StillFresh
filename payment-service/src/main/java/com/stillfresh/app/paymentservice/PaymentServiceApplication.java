package com.stillfresh.app.paymentservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import com.stillfresh.app.paymentservice.config.StripeProperties;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;

@SpringBootApplication(scanBasePackages = { "com.stillfresh.app.paymentservice", "com.stillfresh.app.orderservice", "com.stillfresh.app.sharedentities" })
@EnableDiscoveryClient
@EnableCaching
@EnableConfigurationProperties(StripeProperties.class)
@EnableJpaRepositories(basePackages = "com.stillfresh.app.paymentservice.repository")
@OpenAPIDefinition(info = @Info(title = "Payment Service API", version = "1.0", description = "Documentation for Payment Service API"))
@EnableFeignClients(basePackages = "com.stillfresh.app.paymentservice.client")
public class PaymentServiceApplication {
	public static void main(String[] args) {
		SpringApplication.run(PaymentServiceApplication.class, args);
	}
}
