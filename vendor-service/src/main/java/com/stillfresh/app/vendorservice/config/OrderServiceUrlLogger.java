package com.stillfresh.app.vendorservice.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Component
public class OrderServiceUrlLogger {

    private static final Logger logger = LoggerFactory.getLogger(OrderServiceUrlLogger.class);

    @Value("${vendor.order-service.url:http://order-service:8085}")
    private String orderServiceUrl;

    @PostConstruct
    public void logUrl() {
        logger.info("Resolved vendor.order-service.url={} ", orderServiceUrl);
    }
}

