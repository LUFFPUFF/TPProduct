package com.example.domain.api.ans_api_module.template.services.job.impl;

import com.example.domain.api.ans_api_module.template.dto.job.JobStatisticsDto;
import com.example.domain.api.ans_api_module.template.mapper.StatisticsMapper;
import com.example.domain.api.ans_api_module.template.services.job.JobStatisticsCollector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class JobStatisticsCollectorImpl implements JobStatisticsCollector {


    private static final String STATS_KEY = "jobStatistics";
    private static final String STEP_STATS_PREFIX = "stepStats_";
    private static final String DUPLICATE_SUFFIX = "_dup";

    private static final DateTimeFormatter TIMESTAMP_FORMATTER =
            DateTimeFormatter.ISO_INSTANT.withZone(ZoneId.systemDefault());

    private final StatisticsMapper statisticsMapper;

    @Override
    public void initializeJobStatistics(JobExecution jobExecution) {

        JobParameters jobParameters = jobExecution.getJobParameters();
        Map<String, Object> parameters = jobParameters != null ?
                statisticsMapper.mapParameters(jobParameters) :
                Collections.emptyMap();

        JobStatisticsDto statisticsDto = JobStatisticsDto.builder()
                .jobName(jobExecution.getJobInstance().getJobName())
                .startTime(convertToInstant(jobExecution.getStartTime()))
                .status(jobExecution.getStatus().name())
                .parameters(parameters)
                .build();

        jobExecution.getExecutionContext().put(STATS_KEY, statisticsDto);

    }

    @Override
    public void finalizeJobStatistics(JobExecution jobExecution, long duration) {

        JobStatisticsDto stats = (JobStatisticsDto) jobExecution.getExecutionContext().get(STATS_KEY);

        if (stats == null) {
            stats = JobStatisticsDto.builder()
                    .jobName(jobExecution.getJobInstance().getJobName())
                    .startTime(Instant.now())
                    .status(jobExecution.getStatus().name())
                    .parameters(Collections.emptyMap())
                    .build();
        }

        stats.setEndTime(convertToInstant(jobExecution.getEndTime()));
        stats.setDuration(duration);
        stats.setStatus(jobExecution.getStatus().name());
        stats.setExitStatus(jobExecution.getExitStatus().getExitCode());

        processStepExecutions(jobExecution, stats);

        jobExecution.getExecutionContext().put(STATS_KEY, stats);
    }

    @Override
    public Map<String, Object> getAggregatedStats(JobExecution jobExecution) {
        JobStatisticsDto stats = (JobStatisticsDto) jobExecution.getExecutionContext().get(STATS_KEY);
        if (stats == null) {
            return Collections.emptyMap();
        }
        return statisticsMapper.toFlatMap(stats);
    }

    @Override
    public Map<String, Object> getStepStats(StepExecution stepExecution) {
        JobExecution jobExecution = stepExecution.getJobExecution();
        JobStatisticsDto stats = (JobStatisticsDto) jobExecution.getExecutionContext().get(STATS_KEY);
        if (stats == null) {
            return Collections.emptyMap();
        }

        String stepKey = findStepKey(stats, stepExecution.getStepName());
        if (stepKey == null) {
            return Collections.emptyMap();
        }

        return statisticsMapper.toFlatMap(stats.getStepStatistics().get(stepKey));
    }

    private void processStepExecutions(JobExecution jobExecution, JobStatisticsDto stats) {
        Map<String, Integer> stepNameCount = new HashMap<>();

        jobExecution.getStepExecutions().forEach(step -> {
            String baseStepName = step.getStepName();
            int count = stepNameCount.merge(baseStepName, 1, Integer::sum);

            String stepKey = count > 1 ?
                    STEP_STATS_PREFIX + baseStepName + DUPLICATE_SUFFIX + count :
                    STEP_STATS_PREFIX + baseStepName;

            JobStatisticsDto.StepStatistics stepStats = statisticsMapper.mapStepExecution(step);
            stats.getStepStatistics().put(stepKey, stepStats);
            stats.aggregateStepMetrics(stepStats);
        });
    }

    private String findStepKey(JobStatisticsDto stats, String stepName) {
        return stats.getStepStatistics().keySet().stream()
                .filter(key -> key.equals(STEP_STATS_PREFIX + stepName) ||
                        key.startsWith(STEP_STATS_PREFIX + stepName + DUPLICATE_SUFFIX))
                .findFirst()
                .orElse(null);
    }

    private Instant convertToInstant(LocalDateTime localDateTime) {
        return localDateTime != null ?
                localDateTime.atZone(ZoneId.systemDefault()).toInstant() :
                Instant.now();
    }
}
