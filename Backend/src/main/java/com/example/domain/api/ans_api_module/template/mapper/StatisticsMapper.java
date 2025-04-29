package com.example.domain.api.ans_api_module.template.mapper;

import com.example.domain.api.ans_api_module.template.dto.job.JobStartDto;
import com.example.domain.api.ans_api_module.template.dto.job.JobStatisticsDto;
import jakarta.validation.Valid;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.item.ExecutionContext;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@Mapper(componentModel = "spring")
public interface StatisticsMapper {

    String ISO_DATE_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX";
    DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ofPattern(ISO_DATE_PATTERN)
            .withZone(ZoneId.systemDefault());

    default Map<String, Object> mapParameters(JobParameters parameters) {
        if (parameters == null) {
            return Collections.emptyMap();
        }

        Map<String, Object> result = new HashMap<>();
        parameters.getParameters().forEach((key, param) -> {
            if (param != null && param.getValue() != null) {
                result.put(key, param.getValue());
            }
        });
        return result;
    }

    default JobStatisticsDto mapToJobStatistics(JobStartDto jobStartDto) {
        if (jobStartDto == null) {
            return null;
        }

        return JobStatisticsDto.builder()
                .jobName(jobStartDto.getJobName())
                .startTime(jobStartDto.getStartTime())
                .parameters(mapParameters(jobStartDto.getParameters()))
                .status("STARTED")
                .stepStatistics(new HashMap<>())
                .build();
    }

    default JobStatisticsDto.StepStatistics mapStepExecution(StepExecution stepExecution) {
        if (stepExecution == null) {
            return null;
        }

        return JobStatisticsDto.StepStatistics.builder()
                .stepName(stepExecution.getStepName())
                .readCount((int) stepExecution.getReadCount())
                .writeCount((int) stepExecution.getWriteCount())
                .commitCount((int) stepExecution.getCommitCount())
                .rollbackCount((int) stepExecution.getRollbackCount())
                .processedCount((int) (stepExecution.getReadCount() - stepExecution.getReadSkipCount()))
                .skipCount((int) stepExecution.getProcessSkipCount())
                .errorCount((int) (stepExecution.getFilterCount() + stepExecution.getFailureExceptions().size()))
                .customMetrics(filterBatchMetrics(stepExecution.getExecutionContext()))
                .build();
    }

    default Map<String, Object> filterBatchMetrics(ExecutionContext executionContext) {
        if (executionContext == null) {
            return Collections.emptyMap();
        }

        Map<String, Object> result = new HashMap<>();
        executionContext.entrySet().stream()
                .filter(entry -> entry.getKey() != null && !entry.getKey().startsWith("batch."))
                .forEach(entry -> result.put(entry.getKey(), entry.getValue()));
        return result;
    }

    default Map<String, Object> toFlatMap(JobStatisticsDto jobStats) {
        validateNotNull(jobStats, "Job statistics");

        Map<String, Object> result = new LinkedHashMap<>();

        result.put("jobName", jobStats.getJobName());
        result.put("status", jobStats.getStatus());
        result.put("duration", jobStats.getDuration());
        result.put("startTime", formatInstant(jobStats.getStartTime()));
        result.put("endTime", formatInstant(jobStats.getEndTime()));

        result.put("totalReadCount", jobStats.getTotalReadCount());
        result.put("totalWriteCount", jobStats.getTotalWriteCount());
        result.put("totalProcessedCount", jobStats.getTotalProcessedCount());
        result.put("totalErrorCount", jobStats.getTotalErrorCount());
        result.put("totalSkipCount", jobStats.getTotalSkipCount());

        result.put("parameters", jobStats.getParameters());

        Map<String, Object> stepsMap = new LinkedHashMap<>();
        jobStats.getStepStatistics().forEach((key, stats) ->
                stepsMap.put(key, toFlatMap(stats)));
        result.put("steps", stepsMap);

        return Collections.unmodifiableMap(result);
    }

    default Map<String, Object> toFlatMap(JobStatisticsDto.StepStatistics stats) {
        validateNotNull(stats, "Step statistics");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("stepName", stats.getStepName());
        result.put("readCount", stats.getReadCount());
        result.put("writeCount", stats.getWriteCount());
        result.put("commitCount", stats.getCommitCount());
        result.put("rollbackCount", stats.getRollbackCount());
        result.put("processedCount", stats.getProcessedCount());
        result.put("skipCount", stats.getSkipCount());
        result.put("errorCount", stats.getErrorCount());
        result.put("customMetrics", stats.getCustomMetrics());

        return Collections.unmodifiableMap(result);
    }

    default long calculateErrorCount(StepExecution stepExecution) {
        return stepExecution.getFilterCount() + stepExecution.getFailureExceptions().size();
    }

    default String formatInstant(Instant instant) {
        return instant != null ? ISO_FORMATTER.format(instant) : null;
    }

    private void validateNotNull(Object obj, String name) {
        if (obj == null) {
            throw new IllegalArgumentException(name + " must not be null");
        }
    }
}
