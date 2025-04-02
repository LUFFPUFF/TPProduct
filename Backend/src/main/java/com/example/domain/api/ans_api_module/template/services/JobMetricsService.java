package com.example.domain.api.ans_api_module.template.services;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobParameters;
import org.springframework.stereotype.Service;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class JobMetricsService {

    private final MeterRegistry meterRegistry;

    public void recordJobStart(String jobName, JobParameters jobParameters) {
        meterRegistry.counter("batch.job.start", "job", jobName).increment();
    }

    public void recordJobCompletion(String jobName,
                                    BatchStatus status,
                                    long durationMs,
                                    String exitCode) {
        meterRegistry.timer("batch.job.duration", "job", jobName)
                .record(durationMs, TimeUnit.MILLISECONDS);

        meterRegistry.counter("batch.job.completion",
                        "job", jobName,
                        "status", status.name(),
                        "exitCode", exitCode)
                .increment();
    }
}
