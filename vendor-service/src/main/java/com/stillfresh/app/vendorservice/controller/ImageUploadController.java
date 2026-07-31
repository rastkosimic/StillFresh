package com.stillfresh.app.vendorservice.controller;

import com.stillfresh.app.vendorservice.service.ImageStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
public class ImageUploadController {

    @Autowired
    private ImageStorageService imageStorageService;

    @PostMapping("/vendors/upload-profile-image")
    @PreAuthorize("hasAnyRole('VENDOR', 'VENDOR_ADMIN')")
    public ResponseEntity<Map<String, String>> uploadProfileImage(
            @RequestParam("image") MultipartFile image) throws IOException {
        String imageUrl = imageStorageService.storeFile(image, "vendors");
        return ResponseEntity.ok(Map.of("imageUrl", imageUrl));
    }

    @PostMapping("/images/upload")
    @PreAuthorize("hasAnyRole('VENDOR', 'VENDOR_ADMIN')")
    public ResponseEntity<Map<String, String>> uploadOfferImage(
            @RequestParam("image") MultipartFile image) throws IOException {
        String imageUrl = imageStorageService.storeFile(image, "offers");
        return ResponseEntity.ok(Map.of("imageUrl", imageUrl));
    }
}
