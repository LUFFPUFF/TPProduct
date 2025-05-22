package com.example.domain.api.statistics_module.metrics.service.impl;

import com.example.domain.api.statistics_module.metrics.service.ITextProcessingMetricsService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TextProcessingMetricsServiceImpl implements ITextProcessingMetricsService {

    private final MeterRegistry registry;
    private static final String PREFIX = "text_processing_";

    private static final String TAG_TASK_NAME = "task_name";
    private static final String TAG_GENERATION_TYPE = "generation_type";
    private static final String TAG_VALIDATED_OBJECT = "validated_object";

    @Override
    public void incrementApiRetries(String taskName, String generationType) {
        buildCounter(PREFIX + "api_retries_total", "Total API call retries",
                Tag.of(TAG_TASK_NAME, sanitize(taskName)),
                Tag.of(TAG_GENERATION_TYPE, sanitize(generationType))
        ).increment();
    }

    @Override
    public void incrementEmptyApiResponse(String taskName, String generationType) {
        buildCounter(PREFIX + "empty_api_response_total", "Total empty/null responses from API",
                Tag.of(TAG_TASK_NAME, sanitize(taskName)),
                Tag.of(TAG_GENERATION_TYPE, sanitize(generationType))
        ).increment();
    }

    @Override
    public void incrementValidationFailures(String objectClassName) {
        buildCounter(PREFIX + "validation_failures_total", "Total validation failures",
                Tag.of(TAG_VALIDATED_OBJECT, sanitize(objectClassName))
        ).increment();
    }

    @Override
    public void incrementApiClientCall(String taskName, String generationType) {
        buildCounter(PREFIX + "api_calls_total", "Total calls to the TextProcessingApiClient",
                Tag.of(TAG_TASK_NAME, sanitize(taskName)),
                Tag.of(TAG_GENERATION_TYPE, sanitize(generationType))
        ).increment();
    }

    private Counter.Builder buildCounterBase(String name, String description) {
        return Counter.builder(name).description(description);
    }

    private Counter buildCounter(String name, String description, Tag... tags) {
        return buildCounterBase(name, description)
                .tags(Tags.of(tags))
                .register(registry);
    }

    private String sanitize(String value) {
        if (value == null || value.isEmpty()) {
            return "unknown";
        }
        return value.replaceAll("[^a-zA-Z0-9_\\-]", "_").toLowerCase();
    }
}
