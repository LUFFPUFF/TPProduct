package com.example.domain.api.ans_api_module.generation.service.impl;

import com.example.domain.api.ans_api_module.generation.model.GenerationRequest;
import com.example.domain.api.ans_api_module.generation.model.GenerationResponse;
import com.example.domain.api.ans_api_module.generation.exception.MLException;
import com.example.domain.api.ans_api_module.generation.service.IResilientTextProcessingApiClient;
import com.example.domain.api.ans_api_module.generation.client.TextProcessingApiClient;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class ResilientTextProcessingApiClientImpl implements IResilientTextProcessingApiClient {

    private final TextProcessingApiClient apiClient;
    private final ITextProcessingMetricsService metricsService;
    private final MeterRegistry meterRegistry;
    private final Validator validator;

    private static final int MAX_RETRIES = 3;
    private static final Duration RETRY_DELAY = Duration.ofSeconds(2);
    private static final String PREFIX = "text_processing_";
    private static final String TAG_TASK_NAME = "task_name";
    private static final String TAG_GENERATION_TYPE = "generation_type";
    private static final String TAG_OUTCOME = "outcome";
    private static final String TAG_EXCEPTION_TYPE = "exception_type";

    @Override
    public GenerationResponse generateText(GenerationRequest request, String taskName, String generationType) throws MLException {
        validate(request);

        RetryTemplate retryTemplate = RetryTemplate.builder()
                .maxAttempts(MAX_RETRIES)
                .fixedBackoff(RETRY_DELAY.toMillis())
                .retryOn(MLException.class)
                .withListener(new RetryListener() {
                    @Override
                    public <T, E extends Throwable> void onError(RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {
                        if (context.getRetryCount() < MAX_RETRIES) {
                            metricsService.incrementApiRetries(taskName, generationType);
                            log.warn("Retry attempt {}/{} for task '{}' due to: {}",
                                    context.getRetryCount(), MAX_RETRIES, taskName, throwable.getMessage());
                        }
                    }
                })
                .build();

        return retryTemplate.execute(context -> {
            log.debug("Attempt {}/{} for API call to task '{}'", context.getRetryCount() + 1, MAX_RETRIES, taskName);
            metricsService.incrementApiClientCall(taskName, generationType);

            io.micrometer.core.instrument.Timer.Sample sample = io.micrometer.core.instrument.Timer.start(meterRegistry);
            try {
                GenerationResponse response = apiClient.generateText(request);
                sample.stop(meterRegistry.timer(PREFIX + "api_call_duration_seconds",
                        TAG_TASK_NAME, taskName, TAG_GENERATION_TYPE, generationType, TAG_OUTCOME, "SUCCESS"));

                if (response == null || response.getGeneratedText() == null) {
                    throw new MLException("Empty or null response from AI service for task " + taskName, 500);
                }
                return response;
            } catch (MLException e) {
                sample.stop(meterRegistry.timer(PREFIX + "api_call_duration_seconds",
                        TAG_TASK_NAME, taskName, TAG_GENERATION_TYPE, generationType, TAG_OUTCOME, "FAILURE",
                        TAG_EXCEPTION_TYPE, e.getClass().getSimpleName()));
                throw e;
            } catch (Exception e) {
                sample.stop(meterRegistry.timer(PREFIX + "api_call_duration_seconds",
                        TAG_TASK_NAME, taskName, TAG_GENERATION_TYPE, generationType, TAG_OUTCOME, "FAILURE",
                        TAG_EXCEPTION_TYPE, e.getClass().getSimpleName()));
                throw new MLException("Unexpected error during API call for " + taskName, 500, e);
            }
        });
    }

    private <T> void validate(T object) throws ConstraintViolationException {
        Set<ConstraintViolation<T>> violations = validator.validate(object);
        if (!violations.isEmpty()) {
            String violationMessages = violations.stream()
                    .map(v -> String.format("%s: %s", v.getPropertyPath(), v.getMessage()))
                    .collect(Collectors.joining(", "));

            log.warn("API Request Validation failed for {}: {}", object.getClass().getSimpleName(), violationMessages);
            throw new ConstraintViolationException("API Request Validation failed: " + violationMessages, violations);
        }
    }
}
