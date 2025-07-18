package com.stillfresh.app.vendorservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stillfresh.app.vendorservice.model.Vendor;
import com.stillfresh.app.vendorservice.service.VendorService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(VendorController.class)
public class VendorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private VendorService vendorService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testVendorRegistrationWithoutZipCode_ShouldReturnBadRequest() throws Exception {
        Vendor vendor = new Vendor();
        vendor.setUsername("testvendor");
        vendor.setEmail("test@example.com");
        vendor.setAddress("123 Main St");
        vendor.setPhone("1234567890");
        vendor.setPassword("password123");
        // zipCode is intentionally omitted

        mockMvc.perform(post("/vendors/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(vendor)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Zip code cannot be blank")));
    }

    @Test
    public void testVendorRegistrationWithZipCode_ShouldReturnOk() throws Exception {
        Vendor vendor = new Vendor();
        vendor.setUsername("testvendor");
        vendor.setEmail("test@example.com");
        vendor.setAddress("123 Main St");
        vendor.setPhone("1234567890");
        vendor.setPassword("password123");
        vendor.setZipCode("12345"); // zipCode is provided

        mockMvc.perform(post("/vendors/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(vendor)))
                .andExpect(status().isOk());
    }
} 