package com.example.domain.api.ans_api_module.template.services.job;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.item.ExecutionContext;

import java.util.List;

public interface JobMetricsService {
    void recordJobStart(String jobName, JobParameters parameters);
    void recordJobCompletion(String jobName, BatchStatus status,
                             long duration, String exitCode,
                             ExecutionContext context);
    void recordJobFailure(String jobName, BatchStatus status,
                          List<Throwable> exceptions);
}
