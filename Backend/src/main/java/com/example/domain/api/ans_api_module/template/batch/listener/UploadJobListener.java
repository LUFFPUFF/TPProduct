package com.example.domain.api.ans_api_module.template.batch.listener;

import com.example.domain.api.ans_api_module.template.services.JobMetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class UploadJobListener implements JobExecutionListener {

    private final JobMetricsService metricsService;
    private long startTime;

    @Override
    public void beforeJob(JobExecution jobExecution) {
        startTime = System.currentTimeMillis();
        log.info("Starting job '{}' with parameters: {}",
                jobExecution.getJobInstance().getJobName(),
                jobExecution.getJobParameters());

        metricsService.recordJobStart(
                jobExecution.getJobInstance().getJobName(),
                jobExecution.getJobParameters()
        );
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        long duration = System.currentTimeMillis() - startTime;
        BatchStatus status = jobExecution.getStatus();

        metricsService.recordJobCompletion(
                jobExecution.getJobInstance().getJobName(),
                status,
                duration,
                jobExecution.getExitStatus().getExitCode()
        );

        if (status == BatchStatus.COMPLETED) {
            log.info("Job '{}' completed successfully in {} ms",
                    jobExecution.getJobInstance().getJobName(),
                    duration);
        } else {
            log.error("Job '{}' failed with status {} after {} ms. Exit status: {}",
                    jobExecution.getJobInstance().getJobName(),
                    status,
                    duration,
                    jobExecution.getExitStatus());
        }
    }

}
