package com.example.todo.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.example.todo.exception.BusinessException;

@Service
public class FileStorageService {

    private final Path uploadRoot;

    public FileStorageService(@Value("${app.file.upload-dir:uploads}") String uploadDir) {
        this.uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(uploadRoot);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to initialize upload directory", ex);
        }
    }

    public StoredFile store(MultipartFile multipartFile) {
        if (multipartFile == null || multipartFile.isEmpty()) {
            throw new BusinessException("File is empty.");
        }

        String sanitizedOriginalName = sanitizeOriginalFilename(multipartFile.getOriginalFilename());
        String storedFilename = createStoredFilename(sanitizedOriginalName);
        Path targetFile = resolveSafe(storedFilename);

        try (InputStream in = multipartFile.getInputStream()) {
            Files.copy(in, targetFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to store file", ex);
        }

        return new StoredFile(
                sanitizedOriginalName,
                storedFilename,
                multipartFile.getContentType(),
                multipartFile.getSize());
    }

    public Resource loadAsResource(String storedFilename) {
        Path file = resolveSafe(storedFilename);
        try {
            Resource resource = new UrlResource(file.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new BusinessException("Attachment file not found.");
            }
            return resource;
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load file", ex);
        }
    }

    public void delete(String storedFilename) {
        Path file = resolveSafe(storedFilename);
        try {
            Files.deleteIfExists(file);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to delete file", ex);
        }
    }

    private String sanitizeOriginalFilename(String originalFilename) {
        String rawName = Optional.ofNullable(originalFilename).orElse("file");
        String normalized = StringUtils.cleanPath(rawName);
        String filenameOnly = Paths.get(normalized).getFileName().toString();
        if (!StringUtils.hasText(filenameOnly) || filenameOnly.contains("..")) {
            throw new BusinessException("Invalid file name.");
        }
        return filenameOnly;
    }

    private String createStoredFilename(String originalFilename) {
        int dotIndex = originalFilename.lastIndexOf('.');
        String extension = "";
        if (dotIndex > 0 && dotIndex < originalFilename.length() - 1) {
            extension = originalFilename.substring(dotIndex).toLowerCase(Locale.ROOT);
        }
        return UUID.randomUUID() + extension;
    }

    private Path resolveSafe(String filename) {
        String cleaned = StringUtils.cleanPath(Optional.ofNullable(filename).orElse(""));
        String safeFileName = Paths.get(cleaned).getFileName().toString();
        if (!StringUtils.hasText(safeFileName) || safeFileName.contains("..")) {
            throw new BusinessException("Invalid file path.");
        }

        Path resolved = uploadRoot.resolve(safeFileName).normalize();
        if (!resolved.startsWith(uploadRoot)) {
            throw new BusinessException("Invalid file path.");
        }
        return resolved;
    }

    public record StoredFile(String originalFilename, String storedFilename, String contentType, long size) {
    }
}
