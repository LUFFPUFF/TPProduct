package com.example.domain.api.ans_api_module.template.dto.job;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import org.springframework.batch.core.JobParameters;
import org.springframework.validation.annotation.Validated;

import java.io.Serializable;
import java.time.Instant;

@Data
@Builder
@Validated
public class JobStartDto implements Serializable {
    @NotBlank
    private String jobName;

    @NotNull
    private JobParameters parameters;

    @NotNull
    private Instant startTime;
}
