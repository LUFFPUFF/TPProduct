package com.example.domain.api.ans_api_module.correction_answer.service;

import com.example.domain.api.ans_api_module.correction_answer.config.MLParamsConfig;
import com.example.domain.api.ans_api_module.correction_answer.config.promt.PromptConfig;
import com.example.domain.api.ans_api_module.correction_answer.dto.GenerationRequest;
import com.example.domain.api.ans_api_module.correction_answer.dto.GenerationResponse;
import com.example.domain.api.ans_api_module.correction_answer.exception.MLException;
import com.example.domain.api.statistics_module.aop.annotation.Counter;
import com.example.domain.api.statistics_module.aop.annotation.MeteredOperation;
import com.example.domain.api.statistics_module.aop.annotation.Tag;
import com.example.domain.api.statistics_module.aop.annotation.Timer;
import com.example.domain.api.statistics_module.metrics.service.ITextProcessingMetricsService;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryListener;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@MeteredOperation(prefix = "text_processing_",
        timers = {
                @Timer(name = "process_query_duration_seconds", description = "Duration of processQuery method calls",
                        tags = {
                                @Tag(key = "generation_type", valueSpEL = "#args[1] != null ? #args[1].toString() : 'unknown'")
                        }
                ),
                @Timer(name = "generate_general_answer_duration_seconds", description = "Duration of generateGeneralAnswer method calls")
        },
        counters = {
                @Counter(name = "requests_total", description = "Total requests to text processing service",
                        tags = {
                                @Tag(key = "method_name", valueSpEL = "#target != null ? #target.class.simpleName + '.' + #methodName : ('unknown_class.' + #methodName)")
                        }
                ),
                @Counter(name = "errors_total", description = "Total errors in text processing service",
                        conditionSpEL = "#throwable != null",
                        tags = {
                                @Tag(key = "method_name", valueSpEL = "#target != null ? #target.class.simpleName + '.' + #methodName : ('unknown_class.' + #methodName)"),
                                @Tag(key = "exception_type", valueSpEL = "#throwable != null ? #throwable.getClass().getSimpleName() : 'none'"),
                                @Tag(key = "generation_type_on_error", valueSpEL = "(#methodName == 'processQuery' && #args[1] != null) ? #args[1].toString() : 'N/A'")
                        }
                )
        }
)
public class TextProcessingService {

    private final TextProcessingApiClient apiClient;
    private final MLParamsConfig defaultApiParams;
    private final Validator validator;
    private final ITextProcessingMetricsService metricsService;
    private final MeterRegistry meterRegistry;

    private static final int MAX_RETRIES = 3;
    private static final Duration RETRY_DELAY = Duration.ofSeconds(2);

    private static final String PREFIX = "text_processing_";

    private static final String TAG_TASK_NAME = "task_name";
    private static final String TAG_GENERATION_TYPE = "generation_type";

    public String processQuery(String query, GenerationType generationType)
            throws MLException, ConstraintViolationException {

        validateInput(query);

        return switch (generationType) {
            case CORRECTION -> processCorrection(query);
            case REWRITE -> processRewrite(query);
            case CORRECTION_THEN_REWRITE -> processCorrectionThenRewrite(query);
            default -> throw new IllegalArgumentException("Unsupported generation type: " + generationType);
        };
    }

    public String generateGeneralAnswer(String userQuery, String companyDescription) throws MLException {
        if (companyDescription == null || companyDescription.trim().isEmpty()) {
            throw new MLException("Company description is required for general answer generation", 400);
        }

        String prompt = buildGeneralAnswerPrompt(userQuery, companyDescription);
        log.debug("Generated prompt for general answer: {}", prompt);

        GenerationRequest request = GenerationRequest.builder()
                .prompt(prompt)
                .temperature(defaultApiParams.getTemperature())
                .maxNewTokens(defaultApiParams.getMaxNewTokens())
                .topP(defaultApiParams.getTopP())
                .doSample(defaultApiParams.isDoSample())
                .stream(false)
                .isTextGeneration(true)
                .build();

        io.micrometer.core.instrument.Timer.Sample sample = io.micrometer.core.instrument.Timer.start(meterRegistry);
        String taskName = "general_answer";
        String generationType = "GENERAL_ANSWER";
        GenerationResponse response = null;
        try {
            metricsService.incrementApiClientCall(taskName, generationType);
            response = apiClient.generateText(request);
            sample.stop(meterRegistry.timer(PREFIX + "api_call_duration_seconds",
                    TAG_TASK_NAME, taskName, TAG_GENERATION_TYPE, generationType, "outcome", "SUCCESS"));
        } catch (Exception e) {
            sample.stop(meterRegistry.timer(PREFIX + "api_call_duration_seconds",
                    TAG_TASK_NAME, taskName, TAG_GENERATION_TYPE, generationType, "outcome", "FAILURE",
                    "exception_type", e.getClass().getSimpleName()));
            throw e;
        }


        String generatedText = response.getGeneratedText();
        if (generatedText == null || generatedText.trim().isEmpty()) {
            log.warn("ML service returned empty/null text for general answer generation.");
            metricsService.incrementEmptyApiResponse(taskName, generationType);
            return "";
        }
        return generatedText.trim();
    }

    private String buildGeneralAnswerPrompt(String userQuery, String companyDescription) {
        return "Ты - помощник компании. Ответь на вопрос клиента, используя ТОЛЬКО предоставленную информацию о компании.\n" +
                "Не придумывай информацию, которой нет в описании. Если информация отсутствует, так и скажи.\n\n" +
                "Информация о компании:\n" +
                companyDescription +
                "\n\n" +
                "Вопрос клиента:\n" +
                userQuery +
                "\n\n" +
                "Ответ:";
    }

    private String processCorrection(String query) {
        GenerationRequest request = createGenerationRequest(query);
        GenerationResponse response = retryGenerateTextCall(request, "correction", GenerationType.CORRECTION.name());
        return extractProcessedText(response, query, "correction", GenerationType.CORRECTION.name());
    }

    private String processRewrite(String query) {
        String prompt = "Перепиши корпоративный ответ, сделав его теплее и дружелюбнее. " +
                "Пиши по-человечески, кратко (до 200 символов) и строго выдай только сам ответ — без пояснений: " + query;
        GenerationRequest request = createGenerationRequestRewrite(prompt);
        GenerationResponse response = retryGenerateTextCall(request, "rewrite", GenerationType.REWRITE.name());
        return extractProcessedText(response, query, "rewrite", GenerationType.REWRITE.name());
    }


    private String processCorrectionThenRewrite(String query) {
        String correctedText = processCorrection(query);
        return processRewrite(correctedText);
    }

    private GenerationRequest createGenerationRequest(String prompt) {
        return GenerationRequest.builder()
                .prompt(prompt)
                .temperature(defaultApiParams.getTemperature())
                .maxNewTokens(defaultApiParams.getMaxNewTokens())
                .topP(defaultApiParams.getTopP())
                .doSample(defaultApiParams.isDoSample())
                .stream(defaultApiParams.isStream())
                .isTextGeneration(false)
                .build();
    }

    private GenerationRequest createGenerationRequestRewrite(String prompt) {
        return GenerationRequest.builder()
                .prompt(prompt)
                .temperature(defaultApiParams.getTemperature())
                .maxNewTokens(defaultApiParams.getMaxNewTokens())
                .topP(defaultApiParams.getTopP())
                .doSample(defaultApiParams.isDoSample())
                .stream(defaultApiParams.isStream())
                .isTextGeneration(true)
                .build();
    }

    private String extractProcessedText(GenerationResponse response, String fallbackText, String taskName, String generationType) {
        if (response == null || response.getGeneratedText() == null || response.getGeneratedText().trim().isEmpty()) {
            log.warn("Empty response received for task '{}', returning fallback text", taskName);
            metricsService.incrementEmptyApiResponse(taskName, generationType);
            return fallbackText.trim();
        }
        return response.getGeneratedText().trim();
    }

    private void validateInput(String query) {
        if (query == null || query.trim().isEmpty()) {
            log.warn("Received null or empty client query");
            throw new IllegalArgumentException("Client query cannot be null or empty");
        }
    }

    private GenerationResponse retryGenerateTextCall(GenerationRequest request, String taskName, String generationType) {
        RetryTemplate retryTemplate = RetryTemplate.builder()
                .maxAttempts(MAX_RETRIES)
                .fixedBackoff(RETRY_DELAY.toMillis())
                .retryOn(MLException.class)
                .withListener(new RetryListener() {
                    @Override
                    public <T, E extends Throwable> void onError(RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {
                        if (context.getRetryCount() < MAX_RETRIES -1) {
                            metricsService.incrementApiRetries(taskName, generationType);
                        }
                    }
                })
                .build();

        return retryTemplate.execute(context -> {
            log.debug("Attempt {}/{} for {} task", context.getRetryCount() + 1, MAX_RETRIES, taskName);

            io.micrometer.core.instrument.Timer.Sample sample = io.micrometer.core.instrument.Timer.start(meterRegistry);
            GenerationResponse response = null;
            try {
                validate(request);
                metricsService.incrementApiClientCall(taskName, generationType);
                response = apiClient.generateText(request);
                sample.stop(meterRegistry.timer(PREFIX + "api_call_duration_seconds",
                        TAG_TASK_NAME, taskName, TAG_GENERATION_TYPE, generationType, "outcome", "SUCCESS"));

                log.debug("API Response for {}: {}", taskName,
                        response != null ? response.toString() : "NULL RESPONSE");

                if (response == null || response.getGeneratedText() == null) {
                    throw new MLException("Empty response from AI service for task " + taskName, 500);
                }
                return response;
            } catch (ConstraintViolationException e) {
                sample.stop(meterRegistry.timer(PREFIX + "api_call_duration_seconds",
                        TAG_TASK_NAME, taskName, TAG_GENERATION_TYPE, generationType, "outcome", "VALIDATION_FAILURE",
                        "exception_type", e.getClass().getSimpleName()));
                log.error("Validation failed for {} task: {}", taskName, e.getConstraintViolations());
                throw new MLException("Validation failed during " + taskName, 400, e);
            } catch (MLException e) {
                sample.stop(meterRegistry.timer(PREFIX + "api_call_duration_seconds",
                        TAG_TASK_NAME, taskName, TAG_GENERATION_TYPE, generationType, "outcome", "FAILURE",
                        "exception_type", e.getClass().getSimpleName()));
                throw e;
            } catch (Exception e) {
                sample.stop(meterRegistry.timer(PREFIX + "api_call_duration_seconds",
                        TAG_TASK_NAME, taskName, TAG_GENERATION_TYPE, generationType, "outcome", "FAILURE",
                        "exception_type", e.getClass().getSimpleName()));
                throw new MLException("Unexpected error during API call for " + taskName, 500, e);
            }
        });
    }

    private String buildCorrectionPrompt(String originalText) {
        return String.format(PromptConfig.getDefault().correctionPromptTemplate(),
                sanitizeInput(originalText));
    }

    private String sanitizeInput(String input) {
        return input != null ? input.replace("\"", "'").replace("\n", " ") : "";
    }

    private <T> void validate(T object) throws ConstraintViolationException {
        Set<ConstraintViolation<T>> violations = validator.validate(object);
        if (!violations.isEmpty()) {
            metricsService.incrementValidationFailures(object.getClass().getSimpleName());
            String violationMessages = violations.stream()
                    .map(v -> String.format("%s: %s", v.getPropertyPath(), v.getMessage()))
                    .collect(Collectors.joining(", "));

            log.warn("Validation failed for {}: {}", object.getClass().getSimpleName(), violationMessages);
            throw new ConstraintViolationException("Validation failed: " + violationMessages, violations);
        }
        log.debug("Validation successful for {}", object.getClass().getSimpleName());
    }
}
