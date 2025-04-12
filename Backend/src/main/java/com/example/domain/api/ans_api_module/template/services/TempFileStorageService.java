package com.example.domain.api.ans_api_module.template.services;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jvnet.hk2.annotations.Service;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.Objects;


@Slf4j
@Service
public class TempFileStorageService {

    private final Path tempDirectory;

    public TempFileStorageService() throws IOException {
        this.tempDirectory = Files.createTempDirectory("batch-uploads");
        log.info("Temporary upload directory created at: {}", tempDirectory);
        this.tempDirectory.toFile().deleteOnExit();
    }

    public File storeTempFile(MultipartFile file) throws IOException {
        String originalFilename = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
        if (originalFilename.isBlank()) {
            throw new IllegalArgumentException("File must have a name");
        }

        String timestampedName = System.currentTimeMillis() + "_" + originalFilename;
        Path destination = tempDirectory.resolve(timestampedName);

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, destination, StandardCopyOption.REPLACE_EXISTING);
        }

        log.info("Stored temporary file: {}", destination);
        return destination.toFile();
    }

    public void cleanupFile(File file) throws IOException {
        if (file != null && file.exists()) {
            Files.deleteIfExists(file.toPath());
            log.info("Deleted temporary file: {}", file.getAbsolutePath());
        }
    }

    public void cleanupAll() {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(tempDirectory)) {
            for (Path path : stream) {
                Files.deleteIfExists(path);
                log.info("Deleted file from temp dir: {}", path);
            }
        } catch (IOException e) {
            log.error("Failed to clean up temporary directory", e);
        }
    }
}

