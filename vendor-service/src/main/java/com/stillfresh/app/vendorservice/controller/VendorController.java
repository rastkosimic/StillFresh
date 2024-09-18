package com.stillfresh.app.vendorservice.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.stillfresh.app.vendorservice.model.Vendor;
import com.stillfresh.app.vendorservice.service.VendorService;

@RestController
@RequestMapping("/vendors")
public class VendorController {

    @Autowired
    private VendorService vendorService;

    @PostMapping("/register")
    public ResponseEntity<Vendor> registerVendor(@RequestBody Vendor vendor) {
        Vendor registeredVendor = vendorService.registerVendor(vendor);
        return ResponseEntity.ok(registeredVendor);
    }

    @PostMapping("/login")
    public ResponseEntity<Vendor> login(@RequestParam String email, @RequestParam String password) {
        Vendor authenticatedVendor = vendorService.authenticateVendor(email, password);
        return ResponseEntity.ok(authenticatedVendor);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Vendor> getVendorById(@PathVariable Long id) {
        Vendor vendor = vendorService.getVendorById(id);
        return ResponseEntity.ok(vendor);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> updateVendorProfile(@PathVariable Long id, @RequestBody Vendor vendor) {
        vendorService.updateVendorProfile(id, vendor);
        return ResponseEntity.noContent().build();
    }
}
