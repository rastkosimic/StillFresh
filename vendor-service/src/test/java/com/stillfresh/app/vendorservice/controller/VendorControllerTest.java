package com.stillfresh.app.vendorservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stillfresh.app.sharedentities.dto.CheckAvailabilityRequest;
import com.stillfresh.app.sharedentities.responses.ApiResponse;
import com.stillfresh.app.vendorservice.client.AuthorizationServiceClient;
import com.stillfresh.app.vendorservice.model.Vendor;
import com.stillfresh.app.vendorservice.service.VendorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class VendorControllerTest {

    @Mock
    private VendorService vendorService;

    @Mock
    private AuthorizationServiceClient authorizationServiceClient;

    @InjectMocks
    private VendorController vendorController;

    private Vendor validVendor;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        validVendor = createValidVendor();
        objectMapper = new ObjectMapper();
    }

    @Test
    public void testVendorRegistration_Success_ShouldReturnOk() throws Exception {
        // Given
        when(authorizationServiceClient.checkAvailability(any(CheckAvailabilityRequest.class)))
                .thenReturn(new ApiResponse(true, "Available"));
        when(vendorService.registerVendor(any(Vendor.class))).thenReturn(validVendor);

        // When
        ResponseEntity<?> response = vendorController.registerVendor(validVendor, mock(BindingResult.class));

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody() instanceof ApiResponse);
        ApiResponse apiResponse = (ApiResponse) response.getBody();
        assertTrue(apiResponse.isSuccess());
        assertEquals("Vendor registration initiated. Check your email for verification.", apiResponse.getMessage());

        verify(authorizationServiceClient).checkAvailability(any(CheckAvailabilityRequest.class));
        verify(vendorService).registerVendor(any(Vendor.class));
    }

    @Test
    public void testVendorRegistration_UsernameUnavailable_ShouldReturnConflict() throws Exception {
        // Given
        when(authorizationServiceClient.checkAvailability(any(CheckAvailabilityRequest.class)))
                .thenReturn(new ApiResponse(false, "Username already exists"));

        // When
        ResponseEntity<?> response = vendorController.registerVendor(validVendor, mock(BindingResult.class));

        // Then
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertTrue(response.getBody() instanceof ApiResponse);
        ApiResponse apiResponse = (ApiResponse) response.getBody();
        assertFalse(apiResponse.isSuccess());
        assertEquals("Username already exists", apiResponse.getMessage());

        verify(authorizationServiceClient).checkAvailability(any(CheckAvailabilityRequest.class));
        verify(vendorService, never()).registerVendor(any(Vendor.class));
    }

    @Test
    public void testVendorRegistration_EmailUnavailable_ShouldReturnConflict() throws Exception {
        // Given
        when(authorizationServiceClient.checkAvailability(any(CheckAvailabilityRequest.class)))
                .thenReturn(new ApiResponse(false, "Email already exists"));

        // When
        ResponseEntity<?> response = vendorController.registerVendor(validVendor, mock(BindingResult.class));

        // Then
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertTrue(response.getBody() instanceof ApiResponse);
        ApiResponse apiResponse = (ApiResponse) response.getBody();
        assertFalse(apiResponse.isSuccess());
        assertEquals("Email already exists", apiResponse.getMessage());

        verify(authorizationServiceClient).checkAvailability(any(CheckAvailabilityRequest.class));
        verify(vendorService, never()).registerVendor(any(Vendor.class));
    }

    @Test
    public void testVendorRegistration_AvailabilityCheckFails_ShouldReturnConflict() throws Exception {
        // Given
        when(authorizationServiceClient.checkAvailability(any(CheckAvailabilityRequest.class)))
                .thenReturn(null);

        // When
        ResponseEntity<?> response = vendorController.registerVendor(validVendor, mock(BindingResult.class));

        // Then
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        // When availabilityResponse is null, the controller returns null as body
        assertNull(response.getBody());

        verify(authorizationServiceClient).checkAvailability(any(CheckAvailabilityRequest.class));
        verify(vendorService, never()).registerVendor(any(Vendor.class));
    }

    @Test
    public void testVendorRegistration_ServiceThrowsException_ShouldReturnInternalServerError() throws Exception {
        // Given
        when(authorizationServiceClient.checkAvailability(any(CheckAvailabilityRequest.class)))
                .thenReturn(new ApiResponse(true, "Available"));
        when(vendorService.registerVendor(any(Vendor.class)))
                .thenThrow(new RuntimeException("Database connection failed"));

        // When
        ResponseEntity<?> response = vendorController.registerVendor(validVendor, mock(BindingResult.class));

        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertTrue(response.getBody() instanceof com.stillfresh.app.sharedentities.responses.ErrorResponse);
        com.stillfresh.app.sharedentities.responses.ErrorResponse errorResponse = 
            (com.stillfresh.app.sharedentities.responses.ErrorResponse) response.getBody();
        assertTrue(errorResponse.getErrorMessage().contains("Failed to initiate registration"));

        verify(authorizationServiceClient).checkAvailability(any(CheckAvailabilityRequest.class));
        verify(vendorService).registerVendor(any(Vendor.class));
    }

    @Test
    public void testVendorRegistration_ValidationError_ShouldReturnBadRequest() throws Exception {
        // Given
        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.hasErrors()).thenReturn(true);
        when(bindingResult.getAllErrors()).thenReturn(java.util.Arrays.asList(
            new org.springframework.validation.FieldError("vendor", "username", "Name cannot be blank")
        ));

        // When
        ResponseEntity<?> response = vendorController.registerVendor(validVendor, bindingResult);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody() instanceof com.stillfresh.app.sharedentities.responses.ErrorResponse);
        com.stillfresh.app.sharedentities.responses.ErrorResponse errorResponse = 
            (com.stillfresh.app.sharedentities.responses.ErrorResponse) response.getBody();
        assertTrue(errorResponse.getErrorMessage().contains("Validation failed"));

        verify(authorizationServiceClient, never()).checkAvailability(any(CheckAvailabilityRequest.class));
        verify(vendorService, never()).registerVendor(any(Vendor.class));
    }

    @Test
    public void testAdminRegistration_Success_ShouldReturnOk() throws Exception {
        // Given
        when(vendorService.registerVendor(any(Vendor.class), eq(true))).thenReturn(validVendor);

        // When
        ResponseEntity<String> response = vendorController.registerAdmin(validVendor, mock(BindingResult.class));

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Admin registration successful. Please verify your email.", response.getBody());

        verify(vendorService).registerVendor(any(Vendor.class), eq(true));
    }

    @Test
    public void testAdminRegistration_ValidationError_ShouldReturnBadRequest() throws Exception {
        // Given
        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.hasErrors()).thenReturn(true);
        when(bindingResult.getAllErrors()).thenReturn(java.util.Arrays.asList(
            new org.springframework.validation.FieldError("vendor", "username", "Name cannot be blank")
        ));

        // When
        ResponseEntity<String> response = vendorController.registerAdmin(validVendor, bindingResult);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().contains("Validation failed"));

        verify(authorizationServiceClient, never()).checkAvailability(any(CheckAvailabilityRequest.class));
        verify(vendorService, never()).registerVendor(any(Vendor.class), any(Boolean.class));
    }

    private Vendor createValidVendor() {
        Vendor vendor = new Vendor();
        vendor.setUsername("testvendor");
        vendor.setEmail("test@example.com");
        vendor.setAddress("123 Main Street, City, State");
        vendor.setPhone("1234567890");
        vendor.setPassword("password123");
        vendor.setLatitude(40.7128);
        vendor.setLongitude(-74.0060);
        return vendor;
    }
}