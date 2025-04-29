package com.example.domain.api.ans_api_module.template.batch.listener;

import com.example.domain.api.ans_api_module.template.dto.job.JobCompletionDto;
import com.example.domain.api.ans_api_module.template.dto.job.JobStartDto;
import com.example.domain.api.ans_api_module.template.mapper.StatisticsMapper;
import com.example.domain.api.ans_api_module.template.services.job.JobMetricsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.annotation.AfterJob;
import org.springframework.batch.core.annotation.BeforeJob;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.time.Instant;

@Component
@RequiredArgsConstructor
@Slf4j
@Validated
public class UploadJobListener implements JobExecutionListener {

    private static final String PROCESSING_STATS = "jobStatistics";
    private final JobMetricsService jobMetricsService;
    private final StatisticsMapper jobStatisticsMapper;
    private long startTime;

    @Override
    @BeforeJob
    public void beforeJob(@NotNull JobExecution jobExecution) {
        validateJobExecution(jobExecution);
        startTime = System.currentTimeMillis();

        JobStartDto jobStartDto = JobStartDto.builder()
                .jobName(jobExecution.getJobInstance().getJobName())
                .parameters(jobExecution.getJobParameters())
                .startTime(Instant.now())
                .build();

        jobExecution.getExecutionContext().put(PROCESSING_STATS,
                jobStatisticsMapper.mapToJobStatistics(jobStartDto));

        jobMetricsService.recordJobStart(
                jobStartDto.getJobName(),
                jobStartDto.getParameters()
        );

        log.info("Starting job: {}", jobStartDto.getJobName());
    }

    @Override
    @AfterJob
    public void afterJob(@NotNull JobExecution jobExecution) {
        validateJobExecution(jobExecution);
        long duration = System.currentTimeMillis() - startTime;

        JobCompletionDto completionDto = JobCompletionDto.builder()
                .jobName(jobExecution.getJobInstance().getJobName())
                .status(jobExecution.getStatus())
                .duration(duration)
                .exitCode(jobExecution.getExitStatus().getExitCode())
                .executionContext(jobExecution.getExecutionContext())
                .exceptions(jobExecution.getFailureExceptions())
                .build();

        if (completionDto.getStatus() == BatchStatus.COMPLETED) {
            handleJobCompletion(completionDto);
        } else {
            handleJobFailure(completionDto);
        }
    }

    private void handleJobCompletion(@Valid JobCompletionDto completionDto) {
        jobMetricsService.recordJobCompletion(
                completionDto.getJobName(),
                completionDto.getStatus(),
                completionDto.getDuration(),
                completionDto.getExitCode(),
                completionDto.getExecutionContext()
        );

        log.info("Job '{}' completed successfully in {} ms",
                completionDto.getJobName(),
                completionDto.getDuration());
    }

    private void handleJobFailure(@Valid JobCompletionDto completionDto) {
        jobMetricsService.recordJobFailure(
                completionDto.getJobName(),
                completionDto.getStatus(),
                completionDto.getExceptions()
        );

        log.error("Job '{}' failed with status {} after {} ms. Exit status: {}",
                completionDto.getJobName(),
                completionDto.getStatus(),
                completionDto.getDuration(),
                completionDto.getExitCode());
    }

    private void validateJobExecution(JobExecution jobExecution) {
        if (jobExecution == null) {
            throw new IllegalArgumentException("JobExecution cannot be null");
        }
        if (jobExecution.getJobInstance() == null) {
            throw new IllegalArgumentException("JobInstance cannot be null");
        }
    }

}
