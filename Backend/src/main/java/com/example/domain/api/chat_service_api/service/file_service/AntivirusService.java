package com.example.domain.api.chat_service_api.service.file_service;

import com.example.domain.api.chat_service_api.exception_handler.exception.service.AntivirusException;
import fi.solita.clamav.ClamAVClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
@RequiredArgsConstructor
public class AntivirusService {

    private final ClamAVClient clamAVClient;

    @Async
    public CompletableFuture<Boolean> scanFile(Path filePath) {
        try {
            byte[] fileContent = Files.readAllBytes(filePath);
            byte[] scanResult = clamAVClient.scan(fileContent);

            boolean isClean = ClamAVClient.isCleanReply(scanResult);
            if (!isClean) {
                log.warn("File is infected: {}", filePath);
            }
            return CompletableFuture.completedFuture(isClean);
        } catch (IOException e) {
            log.error("Failed to scan file: {}", filePath, e);
            throw new AntivirusException("Failed to scan file", e);
        }
    }

    @Async
    public CompletableFuture<Boolean> ping() {
        try {
            boolean isAlive = clamAVClient.ping();
            if (!isAlive) {
                log.warn("ClamAV server is not responding");
            }
            return CompletableFuture.completedFuture(isAlive);
        } catch (IOException e) {
            log.error("Failed to ping ClamAV server", e);
            return CompletableFuture.completedFuture(false);
        }
    }
}
