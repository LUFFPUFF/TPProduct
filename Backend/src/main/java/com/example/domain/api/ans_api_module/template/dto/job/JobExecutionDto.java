package com.example.domain.api.ans_api_module.template.dto.job;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.item.ExecutionContext;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class JobExecutionDto {
    @NotBlank
    private String jobName;

    @NotNull
    private JobParameters parameters;

    private BatchStatus status;
    private long duration;
    private String exitCode;
    private ExecutionContext executionContext;
    private List<Throwable> exceptions;
}
