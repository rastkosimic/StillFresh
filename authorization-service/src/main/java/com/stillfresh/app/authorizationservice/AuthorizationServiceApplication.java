package com.stillfresh.app.authorizationservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import com.stillfresh.app.authorizationservice.AuthorizationServiceApplication;
import com.stillfresh.app.authorizationservice.config.JwtConfig;
import com.stillfresh.app.authorizationservice.config.SendGridConfig;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;

@SpringBootApplication(scanBasePackages = "com.stillfresh.app.authorizationservice")
@EnableDiscoveryClient
@EnableCaching
@EnableJpaRepositories(basePackages = "com.stillfresh.app.authorizationservice.repository")
@OpenAPIDefinition(info = @Info(title = "Authorization Service API", version = "1.0", description = "Authorization for Vendor Service API"))
@EnableConfigurationProperties({JwtConfig.class, SendGridConfig.class})
public class AuthorizationServiceApplication {
	
    public static void main(String[] args) {
        SpringApplication.run(AuthorizationServiceApplication.class, args);
    }

}
