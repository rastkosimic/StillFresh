package com.stillfresh.app.userservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;

@SpringBootApplication(scanBasePackages = {
	    "com.stillfresh.app.userservice",
	    "com.stillfresh.app.sharedentities"
	})
@EnableDiscoveryClient
@EnableCaching
@EnableJpaRepositories(basePackages = "com.stillfresh.app.userservice.repository")
@OpenAPIDefinition(info = @Info(title = "User Service API", version = "1.0", description = "Documentation for User Service API"))
@EnableFeignClients(basePackages = "com.stillfresh.app.userservice.client")
public class UserServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}
