package com.example.domain.api.ans_api_module.correction_answer.service;

import com.example.domain.api.ans_api_module.correction_answer.config.MLParamsConfig;
import com.example.domain.api.ans_api_module.correction_answer.config.promt.PromptConfig;
import com.example.domain.api.ans_api_module.correction_answer.dto.GenerationRequest;
import com.example.domain.api.ans_api_module.correction_answer.dto.GenerationResponse;
import com.example.domain.api.ans_api_module.correction_answer.exception.MLException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class TextProcessingService {

    private final TextProcessingApiClient apiClient;
    private final MLParamsConfig defaultApiParams;
    private final Validator validator;

    private static final int MAX_RETRIES = 3;
    private static final Duration RETRY_DELAY = Duration.ofSeconds(2);
    private static final String DEFAULT_RETURN_TEXT = "Unable to process the request";

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

    private String processCorrection(String query) {
        String prompt = buildCorrectionPrompt(query);
        GenerationRequest request = createGenerationRequest(prompt);

        GenerationResponse response = retryGenerateTextCall(request, "correction");
        return extractProcessedText(response, query);
    }

    private String processRewrite(String query) {
        String prompt = buildRewritePrompt("Перепиши следующий корпоративный ответ, сделав его более дружелюбным, теплым и естественным, но при этом не перегруженным деталями. " +
                "Сделай так, чтобы ответ звучал приветливо и было ощущение личного общения. " +
                "Также убедись, что ответ остается коротким и по делу.", query);
        GenerationRequest request = createGenerationRequestRewrite(prompt);

        GenerationResponse response = retryGenerateTextCall(request, "rewrite");
        return extractProcessedText(response, query);
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

    private String extractProcessedText(GenerationResponse response, String fallbackText) {
        if (response == null || response.getGeneratedText() == null || response.getGeneratedText().trim().isEmpty()) {
            log.warn("Empty response received, returning fallback text");
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

    private GenerationResponse retryGenerateTextCall(GenerationRequest request, String taskName) {
        RetryTemplate retryTemplate = RetryTemplate.builder()
                .maxAttempts(MAX_RETRIES)
                .fixedBackoff(RETRY_DELAY.toMillis())
                .retryOn(MLException.class)
                .build();

        return retryTemplate.execute(context -> {
            log.debug("Attempt {}/{} for {} task", context.getRetryCount() + 1, MAX_RETRIES, taskName);
            try {
                validate(request);
                return apiClient.generateText(request);
            } catch (ConstraintViolationException e) {
                log.error("Validation failed for {} task: {}", taskName, e.getConstraintViolations());
                throw new MLException("Validation failed", 400, e);
            }
        });
    }

    private String buildCorrectionPrompt(String originalText) {
        return String.format(PromptConfig.getDefault().correctionPromptTemplate(),
                sanitizeInput(originalText));
    }

    private String buildRewritePrompt(String instruction, String textToRewrite) {
        return String.format(PromptConfig.getDefault().rewritePromptTemplate(),
                sanitizeInput(instruction),
                sanitizeInput(textToRewrite));
    }

    private String sanitizeInput(String input) {
        return input != null ? input.replace("\"", "'").replace("\n", " ") : "";
    }

    private <T> void validate(T object) throws ConstraintViolationException {
        Set<ConstraintViolation<T>> violations = validator.validate(object);
        if (!violations.isEmpty()) {
            String violationMessages = violations.stream()
                    .map(v -> String.format("%s: %s", v.getPropertyPath(), v.getMessage()))
                    .collect(Collectors.joining(", "));

            log.warn("Validation failed for {}: {}", object.getClass().getSimpleName(), violationMessages);
            throw new ConstraintViolationException("Validation failed: " + violationMessages, violations);
        }
        log.debug("Validation successful for {}", object.getClass().getSimpleName());
    }
}
