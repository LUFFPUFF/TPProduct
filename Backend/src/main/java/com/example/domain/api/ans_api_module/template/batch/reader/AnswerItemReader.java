package com.example.domain.api.ans_api_module.template.batch.reader;

import com.example.domain.api.ans_api_module.template.batch.item.*;
import com.example.domain.api.ans_api_module.template.exception.UnsupportedFileTypeException;
import com.example.domain.api.ans_api_module.template.services.TempFileStorageService;
import com.example.domain.api.ans_api_module.template.util.FileType;
import com.example.domain.api.ans_api_module.template.util.FileTypeDetector;
import com.example.domain.api.ans_api_module.template.dto.request.PredefinedAnswerUploadDto;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.mock.web.MockMultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@StepScope
public class AnswerItemReader implements ItemReader<PredefinedAnswerUploadDto> {

    private Iterator<PredefinedAnswerUploadDto> answersIterator;
    private final FileTypeDetector fileTypeDetector;
    private final List<AnswerFileReader> fileReaders;
    private final TempFileStorageService tempFileStorageService;

    @Value("#{jobParameters['inputFilePath']}")
    private String inputFilePath;

    @Value("#{jobParameters['originalFileName']}")
    private String originalFileName;

    @Value("#{jobParameters['contentType']}")
    private String contentType;

    private void validateParameters() {
        log.info("Инициализация AnswerItemReader с параметрами:");
        log.info("inputFilePath: {}", inputFilePath);
        log.info("originalFileName: {}", originalFileName);
        log.info("contentType: {}", contentType);

        if (inputFilePath == null || inputFilePath.isEmpty()) {
            throw new IllegalArgumentException("Путь к файлу не может быть пустым");
        }
    }

    private void validateFile(Path filePath) throws IOException {
        if (!Files.exists(filePath)) {
            throw new FileNotFoundException("Файл не существует по пути: " + filePath);
        }
        if (!Files.isReadable(filePath)) {
            throw new AccessDeniedException("Нет прав на чтение файла: " + filePath);
        }
    }

    @Override
    public PredefinedAnswerUploadDto read() throws Exception {
        if (answersIterator == null) {
            initializeReader();
        }
        return answersIterator.hasNext() ? answersIterator.next() : null;
    }

    private synchronized void initializeReader() throws Exception {
        if (answersIterator != null) return;

        validateParameters();
        Path filePath = Paths.get(inputFilePath);
        validateFile(filePath);

        MultipartFile multipartFile = new MockMultipartFile(
                originalFileName,
                originalFileName,
                contentType,
                Files.newInputStream(filePath)
        );

        FileType fileType = fileTypeDetector.detect(multipartFile);
        AnswerFileReader reader = fileReaders.stream()
                .filter(r -> r.supports(fileType))
                .findFirst()
                .orElseThrow(() -> new UnsupportedFileTypeException(fileType));

        this.answersIterator = reader.read(filePath.toFile()).iterator();
    }

    @PreDestroy
    public void cleanup() {
        try {
            if (inputFilePath != null) {
                Path path = Paths.get(inputFilePath);
                if (Files.exists(path)) {
                    log.info("Удаление временного файла: {}", path);
                    tempFileStorageService.cleanupFile(path.toFile());
                }
            }
        } catch (IOException e) {
            log.error("Ошибка при удалении временного файла", e);
        }
    }
}
