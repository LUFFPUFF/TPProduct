package com.example.domain.api.ans_api_module.template.batch.reader;

import com.example.database.model.ai_module.PredefinedAnswer;
import com.example.domain.api.ans_api_module.template.batch.item.*;
import com.example.domain.api.ans_api_module.template.exception.UnsupportedFileTypeException;
import com.example.domain.api.ans_api_module.template.mapper.PredefinedAnswerMapper;
import com.example.domain.api.ans_api_module.template.util.FileType;
import com.example.domain.api.ans_api_module.template.util.FileTypeDetector;
import com.example.domain.dto.ans_module.predefined_answer.request.PredefinedAnswerUploadDto;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.NonTransientResourceException;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.Iterator;
import java.util.List;

@Slf4j
@Component
@StepScope
@RequiredArgsConstructor
public class AnswerItemReader implements ItemReader<PredefinedAnswerUploadDto> {

    private Iterator<PredefinedAnswerUploadDto> answersIterator;
    private final FileTypeDetector fileTypeDetector;
    private final List<AnswerFileReader> fileReaders;

    @Value("#{jobParameters['file']}")
    private MultipartFile file;

    @PostConstruct
    public void init() {
        FileType fileType = fileTypeDetector.detect(file);
        AnswerFileReader reader = fileReaders.stream()
                .filter(r -> r.supports(fileType))
                .findFirst()
                .orElseThrow(() -> new UnsupportedFileTypeException(fileType));

        List<PredefinedAnswerUploadDto> dtos = reader.read(file);
        this.answersIterator = dtos.iterator();

        log.info("Initialized AnswerItemReader with {} records for file type: {}", dtos.size(), fileType);
    }

    @NotNull
    @Override
    public PredefinedAnswerUploadDto read() {
        try {
            return answersIterator != null && answersIterator.hasNext() ? answersIterator.next() : null;
        } catch (Exception e) {
            log.error("Error reading answer record", e);
            throw new NonTransientResourceException("Failed to read answer record", e);
        }
    }
}
