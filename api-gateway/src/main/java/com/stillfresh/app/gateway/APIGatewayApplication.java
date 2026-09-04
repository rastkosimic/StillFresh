package com.stillfresh.app.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Import;

import com.stillfresh.app.sharedentities.config.ProdConfigGuard;

@SpringBootApplication
@EnableDiscoveryClient
// This application does not scan com.stillfresh.app.sharedentities, so the guard is imported.
@Import(ProdConfigGuard.class)
public class APIGatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(APIGatewayApplication.class, args);
    }
}
