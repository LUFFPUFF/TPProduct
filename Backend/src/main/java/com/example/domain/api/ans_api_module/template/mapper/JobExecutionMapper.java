package com.example.domain.api.ans_api_module.template.mapper;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.batch.core.*;
import org.springframework.batch.item.ExecutionContext;

import java.util.List;

@Mapper(componentModel = "spring")
public interface JobExecutionMapper {

    default JobExecution createNewJobExecution(@NotBlank String jobName,
                                               @NotNull JobParameters parameters) {
        JobInstance jobInstance = new JobInstance(null, jobName);
        return new JobExecution(jobInstance, parameters);
    }

    default JobExecution createCompletedJobExecution(@NotBlank String jobName,
                                                     @NotNull BatchStatus status,
                                                     @PositiveOrZero long duration,
                                                     @NotBlank String exitCode,
                                                     @NotNull ExecutionContext context) {
        JobInstance jobInstance = new JobInstance(null, jobName);
        JobExecution jobExecution = new JobExecution(jobInstance, new JobParameters());

        jobExecution.setStatus(status);
        jobExecution.setExitStatus(new ExitStatus(exitCode));
        jobExecution.setExecutionContext(context);

        return jobExecution;
    }

    default JobExecution createFailedJobExecution(@NotBlank String jobName,
                                                  @NotNull BatchStatus status,
                                                  @NotNull List<Throwable> exceptions) {
        JobInstance jobInstance = new JobInstance(null, jobName);
        JobExecution jobExecution = new JobExecution(jobInstance, new JobParameters());

        jobExecution.setStatus(status);
        if (exceptions != null) {
            for (Throwable exception : exceptions) {
                jobExecution.addFailureException(exception);
            }
        }

        return jobExecution;
    }
}
