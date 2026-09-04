package com.stillfresh.app.authorizationservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import com.stillfresh.app.authorizationservice.AuthorizationServiceApplication;
import com.stillfresh.app.authorizationservice.config.JwtConfig;
import com.stillfresh.app.authorizationservice.config.MailgunConfig;
import com.stillfresh.app.sharedentities.config.ProdConfigGuard;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;

@SpringBootApplication(scanBasePackages = "com.stillfresh.app.authorizationservice")
@EnableDiscoveryClient
@EnableCaching
@EnableFeignClients
@EnableJpaRepositories(basePackages = "com.stillfresh.app.authorizationservice.repository")
@OpenAPIDefinition(info = @Info(title = "Authorization Service API", version = "1.0", description = "Authorization for Vendor Service API"))
@EnableConfigurationProperties({JwtConfig.class, MailgunConfig.class})
// This application does not scan com.stillfresh.app.sharedentities, so the guard is imported.
@Import(ProdConfigGuard.class)
public class AuthorizationServiceApplication {
	
    public static void main(String[] args) {
        SpringApplication.run(AuthorizationServiceApplication.class, args);
    }

}
