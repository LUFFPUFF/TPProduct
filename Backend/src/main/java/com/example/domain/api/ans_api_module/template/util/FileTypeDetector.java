package com.example.domain.api.ans_api_module.template.util;

import com.example.domain.api.ans_api_module.template.exception.UnsupportedFileTypeException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
@RequiredArgsConstructor
public class FileTypeDetector {

    public FileType detect(MultipartFile file) {
        String filename = file.getOriginalFilename();

        return switch (filename.substring(filename.lastIndexOf(".") + 1).toLowerCase()) {
            case "csv" -> FileType.CSV;
            case "json" -> FileType.JSON;
            case "xml" -> FileType.XML;
            default -> throw new UnsupportedFileTypeException(STR."Unsupported format: \{filename}");
        };
    }
}
