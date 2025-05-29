package com.example.domain.api.ans_api_module.generation.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.*;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Component
@ConfigurationProperties(prefix = "ml.api.params")
@Getter
@Setter
@Builder
@AllArgsConstructor
@Validated
public class MLParamsConfig {

    @Builder.Default
    @Min(0) @Max(2)
    private double defaultTemperature = 0.7;

    @Builder.Default
    @Min(10) @Max(4096)
    private int defaultMaxNewTokens = 256;

    @Builder.Default
    @Min(0) @Max(1)
    private double defaultTopP = 0.85;

    @Builder.Default
    private boolean defaultDoSample = true;

    @Builder.Default
    private boolean defaultStream = false;

    private TaskParams correction = TaskParams.builder().build();

    private TaskParams rewrite = TaskParams.builder().build();

    private TaskParams generalAnswer = TaskParams.builder().build();

    @Builder.Default
    @Min(1) @Max(10)
    private int maxRetries = 3;

    @Builder.Default
    private Duration retryDelay = Duration.ofSeconds(2);


    @Getter
    @Setter
    @Builder
    public static class TaskParams {
        private Double temperature;
        private Integer maxNewTokens;
        private Double topP;
        private Boolean doSample;
        private Boolean stream;
        private Boolean isTextGeneration;
    }

    public double getTemperatureForTask(GenerationTaskType type) {
        TaskParams taskParams = getTaskParams(type);
        return (taskParams != null && taskParams.getTemperature() != null) ? taskParams.getTemperature() : defaultTemperature;
    }

    public int getMaxNewTokensForTask(GenerationTaskType type) {
        TaskParams taskParams = getTaskParams(type);
        return (taskParams != null && taskParams.getMaxNewTokens() != null) ? taskParams.getMaxNewTokens() : defaultMaxNewTokens;
    }

    public double getTopPForTask(GenerationTaskType type) {
        TaskParams taskParams = getTaskParams(type);
        return (taskParams != null && taskParams.getTopP() != null) ? taskParams.getTopP() : defaultTopP;
    }

    public boolean isDoSampleForTask(GenerationTaskType type) {
        TaskParams taskParams = getTaskParams(type);
        return (taskParams != null && taskParams.getDoSample() != null) ? taskParams.getDoSample() : defaultDoSample;
    }

    public boolean isStreamForTask(GenerationTaskType type) {
        TaskParams taskParams = getTaskParams(type);
        return (taskParams != null && taskParams.getStream() != null) ? taskParams.getStream() : defaultStream;
    }

    public boolean isTextGenerationForTask(GenerationTaskType type) {
        TaskParams taskParams = getTaskParams(type);
        return (taskParams != null && taskParams.getIsTextGeneration() != null) ? taskParams.getIsTextGeneration() : false;
    }


    private TaskParams getTaskParams(GenerationTaskType type) {
        return switch (type) {
            case CORRECTION -> correction;
            case REWRITE -> rewrite;
            case GENERAL_ANSWER -> generalAnswer;
            default -> null;
        };
    }

    public enum GenerationTaskType {
        CORRECTION, REWRITE, GENERAL_ANSWER
    }

    public MLParamsConfig() {
        this.correction = TaskParams.builder().isTextGeneration(false).build();
        this.rewrite = TaskParams.builder().isTextGeneration(true).build();
        this.generalAnswer = TaskParams.builder().isTextGeneration(true).build();
    }
}
