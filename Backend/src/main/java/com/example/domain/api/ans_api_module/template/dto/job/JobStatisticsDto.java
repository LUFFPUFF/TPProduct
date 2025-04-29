package com.example.domain.api.ans_api_module.template.dto.job;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Builder
@Data
public class JobStatisticsDto implements Serializable {
    @NotNull private String jobName;
    @NotNull private Instant startTime;
    private Instant endTime;
    private long duration;
    @NotBlank private String status;
    private String exitStatus;

    @Builder.Default
    private Map<String, Object> parameters = new HashMap<>();

    @Builder.Default
    private Map<String, StepStatistics> stepStatistics = new HashMap<>();


    private long totalReadCount;
    private long totalWriteCount;
    private long totalProcessedCount;
    private long totalErrorCount;
    private long totalSkipCount;

    public void aggregateStepMetrics(StepStatistics stepStats) {
        this.totalReadCount += stepStats.getReadCount();
        this.totalWriteCount += stepStats.getWriteCount();
        this.totalProcessedCount += stepStats.getProcessedCount();
        this.totalErrorCount += stepStats.getErrorCount();
        this.totalSkipCount += stepStats.getSkipCount();
    }

    @Data
    @Builder
    public static class StepStatistics implements Serializable {
        @NotBlank private String stepName;
        private int readCount;
        private int writeCount;
        private int commitCount;
        private int rollbackCount;
        private int processedCount;
        private int skipCount;
        private int errorCount;

        @Builder.Default
        private Map<String, Object> customMetrics = new HashMap<>();
    }
}
