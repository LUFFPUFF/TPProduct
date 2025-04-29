package com.example.domain.api.ans_api_module.template.util;

import com.example.domain.api.ans_api_module.template.exception.UnsupportedFileTypeException;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;

@Component
public class FileTypeDetector {

    public FileType detect(MultipartFile file) {
        String filename = file.getOriginalFilename();

        if (filename == null || filename.isBlank()) {
            throw new IllegalArgumentException("File name cannot be empty");
        }

        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1) {
            throw new UnsupportedFileTypeException("File has no extension: " + filename);
        }

        String extension = filename.substring(lastDotIndex + 1).toLowerCase();

        return switch (extension) {
            case "csv" -> FileType.CSV;
            case "json" -> FileType.JSON;
            case "xml" -> FileType.XML;
            case "txt" -> FileType.TXT;
            default -> throw new UnsupportedFileTypeException("Unsupported format: " + filename);
        };
    }
}
