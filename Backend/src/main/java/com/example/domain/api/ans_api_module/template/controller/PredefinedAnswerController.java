package com.example.domain.api.ans_api_module.template.controller;

import com.example.domain.api.ans_api_module.template.services.answer.PredefinedAnswerService;
import com.example.domain.api.ans_api_module.template.dto.request.PredefinedAnswerUploadDto;
import com.example.domain.api.ans_api_module.template.dto.response.AnswerResponse;
import com.example.domain.api.ans_api_module.template.dto.response.UploadResultResponse;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/answers")
@RequiredArgsConstructor
@Slf4j
public class PredefinedAnswerController {

    private final PredefinedAnswerService answerService;
    private final JobLauncher jobLauncher;
    private final Job answerUploadJob;

    @PostMapping
    @Transactional
    public ResponseEntity<AnswerResponse> createAnswer(@Valid @RequestBody PredefinedAnswerUploadDto dto) {
        AnswerResponse response = answerService.createAnswer(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/all")
    public ResponseEntity<List<AnswerResponse>> getAllAnswers() {
        List<AnswerResponse> response = answerService.getAllAnswers();
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<AnswerResponse> updateAnswer(
            @PathVariable Integer id,
            @Valid @RequestBody PredefinedAnswerUploadDto dto) {
        AnswerResponse response = answerService.updateAnswer(id, dto);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> deleteAnswer(@PathVariable Integer id) {
        answerService.deleteAnswer(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<AnswerResponse> getAnswerById(@PathVariable Integer id) {
        AnswerResponse response = answerService.getAnswerById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<Page<AnswerResponse>> searchAnswers(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Integer companyId,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<AnswerResponse> response = answerService.searchAnswers(query, companyId, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<List<AnswerResponse>> getAnswersByCategory(
            @PathVariable String category,
            @RequestParam(required = false) Integer companyId) {
        List<AnswerResponse> responses = companyId != null
                ? answerService.getAllAnswers()
                : answerService.getAnswersByCategory(category);
        return ResponseEntity.ok(responses);
    }

    @PostMapping("/upload")
    public ResponseEntity<UploadResultResponse> uploadAnswers(
            @RequestParam("file") MultipartFile file,
            @RequestParam Long companyId,
            @RequestParam String category,
            @RequestParam(defaultValue = "false") String overwrite) {

        try {

            if (file.getContentType() != null && file.getContentType().contains("xml")) {
                String content = new String(file.getBytes());
                if (!content.contains("<title>") || !content.contains("<answer>")) {
                    throw new IllegalArgumentException("XML-файл должен содержать элементы <title> и <answer>");
                }
            }

            if (file.isEmpty()) {
                throw new IllegalArgumentException("Файл не может быть пустым");
            }
            if (companyId == null || companyId <= 0) {
                throw new IllegalArgumentException("Некорректный companyId");
            }
            if (category == null || category.isBlank()) {
                throw new IllegalArgumentException("Категория не может быть пустой");
            }

            Path tempDir = Files.createTempDirectory("upload_");
            Path tempFilePath = tempDir.resolve(Objects.requireNonNull(file.getOriginalFilename()));
            file.transferTo(tempFilePath);

            if (!Files.exists(tempFilePath) || Files.size(tempFilePath) == 0) {
                throw new IllegalStateException("Не удалось сохранить временный файл");
            }

            JobParameters jobParameters = new JobParametersBuilder()
                    .addString("inputFilePath", tempFilePath.toAbsolutePath().toString())
                    .addString("originalFileName", file.getOriginalFilename())
                    .addString("contentType", Objects.requireNonNull(file.getContentType()))
                    .addLong("companyId", companyId)
                    .addString("category", category)
                    .addLong("timestamp", System.currentTimeMillis())
                    .addString("overwrite", overwrite)
                    .toJobParameters();

            log.info("Запуск job с параметрами: {}", jobParameters);

            JobExecution jobExecution = jobLauncher.run(answerUploadJob, jobParameters);

            long writeCount = jobExecution.getStepExecutions().stream()
                    .mapToLong(org.springframework.batch.core.StepExecution::getWriteCount)
                    .sum();

            UploadResultResponse response = UploadResultResponse.builder()
                    .status("SUCCESS")
                    .processedCount((int) writeCount)
                    .build();

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Ошибка при загрузке файла", e);

            UploadResultResponse response = UploadResultResponse.builder()
                    .status("FAILED")
                    .globalErrors(Collections.singletonList(
                            e.getMessage() != null ? e.getMessage() : "Произошла неизвестная ошибка"))
                    .build();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @DeleteMapping("/batch")
    @Transactional
    public ResponseEntity<Void> deleteByCompanyAndCategory(
            @RequestParam Integer companyId,
            @RequestParam String category) {
        answerService.deleteByCompanyIdAndCategory(companyId, category);
        return ResponseEntity.noContent().build();
    }


}
