package com.example.domain.api.ans_api_module.template.batch.job;

import com.example.database.model.ai_module.PredefinedAnswer;
import com.example.domain.api.ans_api_module.template.exception.ValidationException;
import com.example.domain.api.ans_api_module.template.dto.request.PredefinedAnswerUploadDto;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.ItemProcessListener;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AnswerItemProcessListener implements ItemProcessListener<PredefinedAnswerUploadDto, PredefinedAnswer>,
        StepExecutionListener {

    private static final String PROCESSED_COUNT = "processedCount";
    private static final String VALIDATION_ERRORS = "validationErrors";
    private static final String PROCESSING_ERRORS = "processingErrors";

    private final ThreadLocal<StepStatistics> stepStatistics = new ThreadLocal<>();

    @Override
    public void beforeStep(StepExecution stepExecution) {
        stepStatistics.set(new StepStatistics());
        log.info("Step execution started: {}", stepExecution.getStepName());
    }

    @Override
    public void beforeProcess(PredefinedAnswerUploadDto item) {
        log.trace("Processing answer: {}", item.getTitle());
        stepStatistics.get().incrementTotalItems();
    }

    @Override
    public void afterProcess(PredefinedAnswerUploadDto item, @NotNull PredefinedAnswer result) {
        stepStatistics.get().incrementProcessedCount();
        log.debug("Processed answer: {}", item.getTitle());
    }

    @Override
    public void onProcessError(PredefinedAnswerUploadDto item, Exception e) {
        if (e instanceof ValidationException) {
            stepStatistics.get().incrementValidationErrors();
            log.warn("Validation error for answer {}: {}", item.getTitle(), e.getMessage());
        } else {
            stepStatistics.get().incrementProcessingErrors();
            log.error("Processing error for answer {}: {}", item.getTitle(), e.getMessage(), e);
        }
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        StepStatistics stats = stepStatistics.get();
        ExecutionContext context = stepExecution.getExecutionContext();

        context.put(PROCESSED_COUNT, stats.getProcessedCount());
        context.put(VALIDATION_ERRORS, stats.getValidationErrors());
        context.put(PROCESSING_ERRORS, stats.getProcessingErrors());

        log.info("Step completed. Statistics: {}", stats);
        stepStatistics.remove();

        return stepExecution.getExitStatus();
    }

    @Data
    private static class StepStatistics {
        private int totalItems;
        private int processedCount;
        private int validationErrors;
        private int processingErrors;

        public void incrementTotalItems() {
            totalItems++;
        }

        public void incrementProcessedCount() {
            processedCount++;
        }

        public void incrementValidationErrors() {
            validationErrors++;
        }

        public void incrementProcessingErrors() {
            processingErrors++;
        }

        @Override
        public String toString() {
            return String.format("Total: %d, Processed: %d, Validation Errors: %d, Processing Errors: %d",
                    totalItems, processedCount, validationErrors, processingErrors);
        }
    }
}
