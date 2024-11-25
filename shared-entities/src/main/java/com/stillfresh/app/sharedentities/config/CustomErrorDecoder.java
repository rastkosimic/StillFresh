package com.stillfresh.app.sharedentities.config;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

import org.springframework.http.HttpStatus;

import com.stillfresh.app.sharedentities.exceptions.ConflictException;
import com.stillfresh.app.sharedentities.responses.ApiResponse;

import feign.Response;
import feign.codec.ErrorDecoder;


public class CustomErrorDecoder implements ErrorDecoder {

    private final ErrorDecoder defaultDecoder = new Default();
    private final ObjectMapper objectMapper;

    public CustomErrorDecoder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Exception decode(String methodKey, Response response) {
        if (response.status() == HttpStatus.CONFLICT.value()) {
            try {
                // Deserialize the response body into ApiResponse
                ApiResponse errorResponse = objectMapper.readValue(response.body().asInputStream(), ApiResponse.class);
                // Create a ConflictException with the error message
                return new ConflictException(errorResponse.getMessage());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return defaultDecoder.decode(methodKey, response);
    }
}
