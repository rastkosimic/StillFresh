package com.stillfresh.app.notificationservice.controller;

import com.stillfresh.app.notificationservice.service.FcmTokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/fcm-token")
public class NotificationController {

    @Autowired
    private FcmTokenService fcmTokenService;

    @PostMapping("/register")
    public ResponseEntity<String> registerToken(
            @RequestParam String userId,
            @RequestParam String token) {
        return fcmTokenService.registerToken(userId, token);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<?> getToken(@PathVariable String userId) {
        return fcmTokenService.getToken(userId);
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<String> deleteToken(@PathVariable String userId) {
        return fcmTokenService.deleteToken(userId);
    }
}
