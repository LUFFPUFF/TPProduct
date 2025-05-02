package com.example.ui.controller;

import com.example.domain.api.ans_api_module.template.dto.request.PredefinedAnswerUploadDto;
import com.example.domain.api.ans_api_module.template.dto.response.AnswerResponse;
import com.example.domain.api.ans_api_module.template.dto.response.UploadResultResponse;
import com.example.domain.api.ans_api_module.template.services.answer.PredefinedAnswerService;
import com.example.domain.api.company_module.service.CompanyService;
import com.example.domain.dto.CompanyWithMembersDto;
import com.example.ui.dto.answer.UiPredefinedAnswerDto;
import com.example.ui.dto.answer.UiPredefinedAnswerRequest;
import com.example.ui.mapper.answer.UIPredefinedAnswerMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
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
@RequestMapping("/api/ui/predefined-answers")
@RequiredArgsConstructor
@Slf4j
public class PredefinedAnswerUIController {

    private final PredefinedAnswerService answerService;
    private final UIPredefinedAnswerMapper uiAnswerMapper;
    private final CompanyService companyService;

    private final JobLauncher jobLauncher;
    private final Job answerUploadJob;

    //TODO получать информацию из security
    private Integer getCurrentUserCompanyId() {
        return 2;
    }

    private Integer getCurrentUserId() {
        return 1;
    }

    /**
     * Создает новый шаблонный ответ.
     * <p>POST /api/ui/predefined-answers
     *
     * @param dto UI DTO с данными нового ответа
     * @return UI DTO созданного ответа.
     */
    @PostMapping
    public ResponseEntity<UiPredefinedAnswerDto> createAnswer(@Valid @RequestBody UiPredefinedAnswerRequest dto) {
        Integer userCompanyId = getCurrentUserCompanyId();

        PredefinedAnswerUploadDto serviceDto = uiAnswerMapper.toServiceDto(dto);

        CompanyWithMembersDto company = companyService.findCompanyWithId(userCompanyId);

        serviceDto.setCompanyDto(company.getCompany());

        AnswerResponse serviceResponse = answerService.createAnswer(serviceDto);
        return ResponseEntity.ok(uiAnswerMapper.toUiDto(serviceResponse));
    }

    /**
     * Обновляет существующий шаблонный ответ.
     * <p>PUT /api/ui/predefined-answers/{id}
     *
     * @param id ID обновляемого ответа.
     * @param dto UI DTO с обновленными данными.
     * @return UI DTO обновленного ответа.
     */
    @PutMapping("/{id}")
    public ResponseEntity<UiPredefinedAnswerDto> updateAnswer(
            @PathVariable Integer id,
            @Valid @RequestBody UiPredefinedAnswerRequest dto) {

        Integer userCompanyId = getCurrentUserCompanyId();

        PredefinedAnswerUploadDto serviceDto = uiAnswerMapper.toServiceDto(dto);
        CompanyWithMembersDto company = companyService.findCompanyWithId(userCompanyId);

        serviceDto.setCompanyDto(company.getCompany());

        AnswerResponse serviceResponse = answerService.updateAnswer(id, serviceDto);

        return ResponseEntity.ok(uiAnswerMapper.toUiDto(serviceResponse));

    }

    /**
     * Удаляет существующий шаблонный ответ.
     * <p>DELETE /api/ui/predefined-answers/{id}
     *
     * @param id ID удаляемого ответа.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAnswer(@PathVariable Integer id) {
        answerService.deleteAnswer(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Получает шаблонный ответ по ID.
     * <p>GET /api/ui/predefined-answers/{id}
     *
     * @param id ID ответа.
     * @return UI DTO ответа.
     */
    @GetMapping("/{id}")
    public ResponseEntity<UiPredefinedAnswerDto> getAnswerById(@PathVariable Integer id) {
        AnswerResponse serviceResponse = answerService.getAnswerById(id);
        return ResponseEntity.ok(uiAnswerMapper.toUiDto(serviceResponse));
    }

    /**
     * Получает список шаблонных ответов по категории для компании текущего оператора.
     * <p>GET /api/ui/predefined-answers/my/category/{category}
     *
     * @param category Категория ответов.
     * @return Список UIPredefinedAnswerDto.
     */
    @GetMapping("/my/category/{category}")
    public ResponseEntity<List<UiPredefinedAnswerDto>> getMyCompanyAnswersByCategory(
            @PathVariable String category) {

        String companyName = "DialogX"; //TODO Тестовое имя - брать из security
        List<AnswerResponse> serviceResponses = answerService.getAnswersByCategory(category);
        List<AnswerResponse> filteredResponses = serviceResponses.stream()
                .filter(ans -> ans.getCompanyName() != null && ans.getCompanyName().equals(companyName))
                .toList();

        List<UiPredefinedAnswerDto> uiResponses = uiAnswerMapper.toUiDtoList(filteredResponses);
        return ResponseEntity.ok(uiResponses);
    }

    /**
     * Загружает файл с шаблонными ответами для компании текущего оператора
     * <p>POST /api/ui/predefined-answers/upload
     *
     * @param file Файл для загрузки.
     * @param category Категория для ответов из файла.
     * @param overwrite Флаг, указывающий, нужно ли перезаписывать существующие ответы в этой категории.
     * @return Результат загрузки.
     */
    @PostMapping("/upload")
    public ResponseEntity<UploadResultResponse> uploadAnswers(
            @RequestParam("file") MultipartFile file,
            @RequestParam String category,
            @RequestParam(defaultValue = "false") String overwrite) {
        Integer currentUserId = getCurrentUserId();
        Integer userCompanyId = getCurrentUserCompanyId();

        try {
            if (file == null || file.isEmpty()) {
                log.warn("Upload file is empty or null.");
                throw new IllegalArgumentException("Файл не может быть пустым");
            }

            if (category == null || category.isBlank()) {
                log.warn("Upload category is blank.");
                throw new IllegalArgumentException("Категория не может быть пустой");
            }

            Path tempDir = Files.createTempDirectory("upload_answers_");
            Path tempFilePath = tempDir.resolve(Objects.requireNonNull(file.getOriginalFilename()));
            file.transferTo(tempFilePath);

            if (!Files.exists(tempFilePath) || Files.size(tempFilePath) == 0) {
                log.error("Failed to save temporary file or it's empty after transfer.");
                throw new IllegalStateException("Не удалось сохранить временный файл или он пуст.");
            }

            JobParameters jobParameters = new JobParametersBuilder()
                    .addString("inputFilePath", tempFilePath.toAbsolutePath().toString())
                    .addString("originalFileName", file.getOriginalFilename())
                    .addString("contentType", Objects.requireNonNull(file.getContentType()))
                    .addLong("companyId", userCompanyId.longValue())
                    .addString("category", category)
                    .addLong("userId", currentUserId.longValue())
                    .addLong("timestamp", System.currentTimeMillis())
                    .addString("overwrite", overwrite)
                    .toJobParameters();

            JobExecution jobExecution = jobLauncher.run(answerUploadJob, jobParameters);

            long writeCount = jobExecution.getStepExecutions().stream()
                    .mapToLong(org.springframework.batch.core.StepExecution::getWriteCount)
                    .sum();

            UploadResultResponse response = UploadResultResponse.builder()
                    .status(jobExecution.getStatus().name())
                    .processedCount((int) writeCount)
                    .build();

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("File upload request validation failed: {}", e.getMessage());
            UploadResultResponse response = UploadResultResponse.builder()
                    .status("VALIDATION_FAILED")
                    .globalErrors(Collections.singletonList(e.getMessage()))
                    .build();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (IllegalStateException e) {
            log.error("File upload process error: {}", e.getMessage());
            UploadResultResponse response = UploadResultResponse.builder()
                    .status("UPLOAD_FAILED")
                    .globalErrors(Collections.singletonList(e.getMessage()))
                    .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        } catch (Exception e) {
            log.error("Unexpected error during file upload", e);
            UploadResultResponse response = UploadResultResponse.builder()
                    .status("FAILED")
                    .globalErrors(Collections.singletonList(
                            e.getMessage() != null ? e.getMessage() : "Произошла неизвестная ошибка"))
                    .build();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

}
