package com.example.domain.api.ans_api_module.template.services.job.impl;

import com.example.domain.api.ans_api_module.template.mapper.JobExecutionMapper;
import com.example.domain.api.ans_api_module.template.services.job.JobMetricsService;
import com.example.domain.api.ans_api_module.template.services.job.JobStatisticsCollector;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.jvnet.hk2.annotations.Service;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Component
@Validated
@RequiredArgsConstructor
public class JobMetricsServiceImpl implements JobMetricsService {

    private final JobStatisticsCollector statisticsCollector;
    private final JobExecutionMapper jobExecutionMapper;

    @Override
    public void recordJobStart(String jobName,
                               JobParameters parameters) {
        JobExecution jobExecution = jobExecutionMapper.createNewJobExecution(jobName, parameters);
        statisticsCollector.initializeJobStatistics(jobExecution);
    }

    @Override
    public void recordJobCompletion(String jobName,
                                    BatchStatus status,
                                    long duration,
                                    String exitCode,
                                    ExecutionContext context) {
        JobExecution jobExecution = jobExecutionMapper.createCompletedJobExecution(
                jobName, status, duration, exitCode, context);

        statisticsCollector.finalizeJobStatistics(jobExecution, duration);
    }

    @Override
    public void recordJobFailure(String jobName,
                                 BatchStatus status,
                                 List<Throwable> exceptions) {
        JobExecution jobExecution = jobExecutionMapper.createFailedJobExecution(
                jobName, status, exceptions);

        statisticsCollector.finalizeJobStatistics(jobExecution, 0);
    }
}
