package com.example.domain.api.ans_api_module.template.dto.job;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Builder;
import lombok.Data;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Data
@Builder
@Validated
public class JobCompletionDto {

    @NotBlank private String jobName;
    @NotNull private BatchStatus status;
    @PositiveOrZero private long duration;
    @NotBlank private String exitCode;
    private ExecutionContext executionContext;

    @NotNull
    private List<Throwable> exceptions;

}
