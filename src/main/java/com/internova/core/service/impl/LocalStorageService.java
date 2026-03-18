package com.internova.core.service.impl;

import com.internova.core.service.StorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class LocalStorageService implements StorageService {

    private final Path rootLocation;
    private final String publicBaseUrl;

    public LocalStorageService(
            @Value("${internova.storage.upload-dir:uploads}") String uploadDir,
            @Value("${internova.storage.public-base-url:http://localhost:8080}") String publicBaseUrl) {
        this.rootLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        this.publicBaseUrl = trimTrailingSlash(publicBaseUrl);
        try {
            Files.createDirectories(rootLocation);
            Files.createDirectories(rootLocation.resolve("resumes"));
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize storage", e);
        }
    }

    @Override
    public String store(MultipartFile file, String pathPrefix) {
        String contentType = file.getContentType();
        if (contentType == null || !contentType.equals("application/pdf")) {
            throw new IllegalArgumentException("Invalid file type. Only PDF uploads are permitted.");
        }

        try {
            if (file.isEmpty()) {
                throw new IllegalArgumentException("Failed to store empty file.");
            }

            String normalizedPrefix = normalizePathPrefix(pathPrefix);
            Path targetDirectory = rootLocation.resolve(normalizedPrefix).normalize();
            Files.createDirectories(targetDirectory);

            String filename = UUID.randomUUID() + ".pdf";
            Path destinationFile = targetDirectory.resolve(filename).normalize().toAbsolutePath();
            if (!destinationFile.getParent().equals(targetDirectory.toAbsolutePath())) {
                throw new SecurityException("Cannot store file outside current directory.");
            }

            Files.copy(file.getInputStream(), destinationFile, StandardCopyOption.REPLACE_EXISTING);

            StringBuilder urlBuilder = new StringBuilder(publicBaseUrl)
                    .append("/uploads/");
            if (!normalizedPrefix.isBlank()) {
                urlBuilder.append(normalizedPrefix).append("/");
            }
            urlBuilder.append(filename);

            return urlBuilder.toString();
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file.", e);
        }
    }

    private String normalizePathPrefix(String pathPrefix) {
        if (!StringUtils.hasText(pathPrefix)) {
            return "";
        }

        String cleaned = StringUtils.cleanPath(pathPrefix).replace('\\', '/');
        if (cleaned.startsWith("../") || cleaned.contains("/../") || cleaned.equals("..")) {
            throw new RuntimeException("Invalid storage path prefix.");
        }
        return cleaned.replaceAll("^/+|/+$", "");
    }

    private String trimTrailingSlash(String baseUrl) {
        if (!StringUtils.hasText(baseUrl)) {
            throw new IllegalArgumentException("Storage public base URL must not be blank.");
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }
}