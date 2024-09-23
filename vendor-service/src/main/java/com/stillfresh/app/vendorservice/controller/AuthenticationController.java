package com.stillfresh.app.vendorservice.controller;

import com.stillfresh.app.vendorservice.model.AuthenticationRequest;
import com.stillfresh.app.vendorservice.security.JwtUtil;
import com.stillfresh.app.vendorservice.service.CustomVendorDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthenticationController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private CustomVendorDetailsService vendorDetailsService;

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody AuthenticationRequest authenticationRequest) throws Exception {
        try {
            // Use email instead of username for authentication
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(authenticationRequest.getEmail(), authenticationRequest.getPassword())
            );
        } catch (Exception e) {
            throw new Exception("Incorrect email or password", e);
        }

        // Use email for vendor lookup
        final UserDetails vendorDetails = vendorDetailsService.loadUserByUsername(authenticationRequest.getEmail());
        final String jwt = jwtUtil.generateToken(vendorDetails);

        return ResponseEntity.ok(jwt);
    }
}
