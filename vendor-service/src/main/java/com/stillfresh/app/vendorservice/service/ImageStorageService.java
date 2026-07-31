package com.stillfresh.app.vendorservice.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class ImageStorageService {

    @Value("${app.upload.dir:/app/uploads}")
    private String uploadDir;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    public String storeFile(MultipartFile file, String subfolder) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File must not be empty");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Only image files are allowed");
        }

        String extension = resolveExtension(file.getOriginalFilename(), contentType);
        String filename = UUID.randomUUID().toString() + "." + extension;

        Path targetDir = Paths.get(uploadDir, subfolder);
        Files.createDirectories(targetDir);

        Path targetPath = targetDir.resolve(filename);
        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

        return baseUrl + "/vendors/uploads/" + subfolder + "/" + filename;
    }

    private String resolveExtension(String originalFilename, String contentType) {
        if (originalFilename != null && originalFilename.contains(".")) {
            String ext = originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase();
            if (!ext.isEmpty()) {
                return ext;
            }
        }
        switch (contentType) {
            case "image/jpeg": return "jpg";
            case "image/png":  return "png";
            case "image/gif":  return "gif";
            case "image/webp": return "webp";
            default:           return "jpg";
        }
    }
}
