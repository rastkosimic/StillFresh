package com.stillfresh.app.userservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;

@SpringBootApplication(scanBasePackages = "com.stillfresh.app.userservice")
@EnableDiscoveryClient
@EnableJpaRepositories(basePackages = "com.stillfresh.app.userservice.repository")
@OpenAPIDefinition(info = @Info(title = "User Service API", version = "1.0", description = "Documentation for User Service API"))
public class UserServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}
