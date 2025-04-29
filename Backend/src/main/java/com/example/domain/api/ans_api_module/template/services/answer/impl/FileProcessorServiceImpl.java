package com.example.domain.api.ans_api_module.template.services.answer.impl;

import com.example.database.repository.ai_module.PredefinedAnswerRepository;
import com.example.domain.api.ans_api_module.template.exception.InvalidFileFormatException;
import com.example.domain.api.ans_api_module.template.services.answer.FileProcessorService;
import com.example.domain.api.ans_api_module.template.util.FileType;
import com.example.domain.api.ans_api_module.template.util.FileTypeDetector;
import com.example.domain.api.ans_api_module.template.dto.request.UploadFileRequest;
import com.example.domain.api.ans_api_module.template.dto.response.UploadResultResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileProcessorServiceImpl implements FileProcessorService {

    private final JobLauncher jobLauncher;
    private final Job answerUploadJob;
    private final FileTypeDetector fileTypeDetector;
    private final PredefinedAnswerRepository answerRepository;

    @Override
    public UploadResultResponse processFileUpload(UploadFileRequest request) {
        try {
            validateFile(request.getFile());
            FileType fileType = detectFileType(request.getFile());
            log.debug("Processing file of type: {}", fileType);

            cleanupExistingAnswersIfNeeded(request);

            JobParameters jobParameters = buildJobParameters(request);
            JobExecution jobExecution = jobLauncher.run(answerUploadJob, jobParameters);

            return buildUploadResult(jobExecution);
        } catch (Exception e) {
            log.error("File processing failed", e);
            return buildErrorResponse(e);
        }
    }

    private void cleanupExistingAnswersIfNeeded(UploadFileRequest request) {
        if (request.isOverwriteExisting()) {
            int deletedCount = answerRepository.deleteByCompanyIdAndCategory(
                    request.getCompanyId(),
                    request.getCategory()
            );
            log.debug("Deleted {} existing answers for company {} and category {}",
                    deletedCount, request.getCompanyId(), request.getCategory());
        }
    }

    private JobParameters buildJobParameters(UploadFileRequest request) {
        return new JobParametersBuilder()
                .addString("filePath", request.getFile().getName())
                .addLong("companyId", Long.valueOf(request.getCompanyId()))
                .addString("category", request.getCategory())
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();
    }

    @SuppressWarnings("unchecked")
    private UploadResultResponse buildUploadResult(JobExecution jobExecution) {
        Map<String, Object> jobStats = jobExecution.getExecutionContext()
                .get("processingStats", Map.class);

        return UploadResultResponse.builder()
                .processedCount(getSafeInteger(jobStats, "processedCount"))
                .duplicatesCount(getSafeInteger(jobStats, "duplicatesCount"))
                .globalErrors(extractGlobalErrors(jobExecution))
                .rowErrors(getSafeMap(jobStats, "rowErrors"))
                .status(jobExecution.getStatus().toString())
                .build();
    }

    private int getSafeInteger(Map<String, Object> map, String key) {
        if (map == null) {
            System.out.println("map is null");
        }

        try {
            Object value = map.getOrDefault(key, 0);
            return value instanceof Integer ? (Integer) value : 0;
        } catch (Exception e) {
            log.warn("Failed to parse integer value for key: {}", key, e);
            return 0;
        }
    }

    @SuppressWarnings("unchecked")
    private Map<Integer, String> getSafeMap(Map<String, Object> map, String key) {
        try {
            Object value = map.getOrDefault(key, Collections.emptyMap());
            return value instanceof Map ? (Map<Integer, String>) value : Collections.emptyMap();
        } catch (Exception e) {
            log.warn("Failed to parse map value for key: {}", key, e);
            return Collections.emptyMap();
        }
    }

    private List<String> extractGlobalErrors(JobExecution jobExecution) {
        return jobExecution.getAllFailureExceptions().stream()
                .map(Throwable::getMessage)
                .collect(Collectors.toList());
    }

    private UploadResultResponse buildErrorResponse(Exception e) {
        return UploadResultResponse.builder()
                .globalErrors(List.of("File processing failed: " + e.getMessage()))
                .status("FAILED")
                .build();
    }

    @Override
    public void validateFile(MultipartFile file) {
        //TODO требует реализации
    }

    @Override
    public FileType detectFileType(MultipartFile file) {
        try {

            FileType fileType = fileTypeDetector.detect(file);

            System.out.println(fileType.name());

            return fileTypeDetector.detect(file);
        } catch (Exception e) {
            throw new InvalidFileFormatException("Unsupported file type", e);
        }
    }
}
