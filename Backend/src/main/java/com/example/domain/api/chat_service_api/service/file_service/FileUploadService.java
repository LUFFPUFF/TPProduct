package com.example.domain.api.chat_service_api.service.file_service;

import com.example.domain.api.chat_service_api.config.ValidationConfig;
import com.example.domain.exception_handler.chat_module.FileValidationException;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Data
@Slf4j
public class FileUploadService {

    private final ValidationConfig validationConfig;
    private final Map<Path, String> mimeCache = new ConcurrentHashMap<>();

    @Async
    public void validateFile(Path filePath) throws FileValidationException {
        String fileType = getFileType(filePath);
        validateFileType(fileType);
        validateFileSize(filePath);
    }

    private String getFileType(Path filePath) throws FileValidationException {
        return mimeCache.computeIfAbsent(filePath, path -> {
            try {
                return Files.probeContentType(path);
            } catch (IOException e) {
                throw new FileValidationException("Failed to determine file type", e);
            }
        });
    }

    private void validateFileType(String fileType) throws FileValidationException {
        if (!validationConfig.getAllowedFileTypes().contains(fileType)) {
            throw new FileValidationException("File type not allowed. Allowed types: " + validationConfig.getAllowedFileTypes());
        }
    }

    private void validateFileSize(Path filePath) throws FileValidationException {
        try {
            long fileSize = Files.size(filePath);
            long maxFileSize = parseFileSize(validationConfig.getMaxFileSize());
            if (fileSize > maxFileSize) {
                throw new FileValidationException("File size exceeds limit. Max size: " + validationConfig.getMaxFileSize());
            }
        } catch (IOException e) {
            throw new FileValidationException("Failed to read file size", e);
        }
    }

    private long parseFileSize(String size) {
        size = size.toUpperCase();
        if (size.endsWith("MB")) {
            return Long.parseLong(size.replace("MB", "")) * 1024 * 1024;
        } else if (size.endsWith("KB")) {
            return Long.parseLong(size.replace("KB", "")) * 1024;
        } else if (size.endsWith("B")) {
            return Long.parseLong(size.replace("B", ""));
        } else if (size.endsWith("GB")) {
            return Long.parseLong(size.replace("GB", "")) * 1024 * 1024 * 1024;
        }
        throw new IllegalArgumentException("Invalid file size format: " + size);
    }
}
