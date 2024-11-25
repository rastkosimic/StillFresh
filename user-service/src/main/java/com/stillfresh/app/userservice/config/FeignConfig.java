package com.stillfresh.app.userservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stillfresh.app.sharedentities.config.CustomErrorDecoder;

import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignConfig {

    @Bean
    public ErrorDecoder errorDecoder(ObjectMapper objectMapper) {
        return new CustomErrorDecoder(objectMapper);
    }
}
