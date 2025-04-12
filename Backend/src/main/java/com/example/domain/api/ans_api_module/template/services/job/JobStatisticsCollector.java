package com.example.domain.api.ans_api_module.template.services.job;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;

import java.util.Map;

public interface JobStatisticsCollector {
    void initializeJobStatistics(JobExecution jobExecution);
    void finalizeJobStatistics(JobExecution jobExecution, long duration);
    Map<String, Object> getAggregatedStats(JobExecution jobExecution);
    Map<String, Object> getStepStats(StepExecution stepExecution);
}
