package com.example.domain.api.ans_api_module.template.batch.job;


import com.example.database.model.ai_module.PredefinedAnswer;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.ItemWriteListener;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AnswerItemWriteListener implements ItemWriteListener<PredefinedAnswer>, StepExecutionListener {

    private static final String WRITTEN_COUNT = "writtenCount";
    private static final String DUPLICATES_COUNT = "duplicatesCount";
    private static final String WRITE_ERRORS = "writeErrors";

    private final ThreadLocal<WriteStatistics> writeStatistics = new ThreadLocal<>();

    @Override
    public void beforeStep(StepExecution stepExecution) {
        writeStatistics.set(new WriteStatistics());
        log.info("Write phase initialized for step: {}", stepExecution.getStepName());
    }

    @Override
    public void beforeWrite(Chunk<? extends PredefinedAnswer> chunk) {
        log.debug("Preparing to write chunk of {} answers", chunk.size());
        writeStatistics.get().incrementTotalChunks();
    }

    @Override
    public void afterWrite(Chunk<? extends PredefinedAnswer> chunk) {
        writeStatistics.get().incrementWrittenCount(chunk.size());
        log.debug("Successfully wrote chunk of {} answers", chunk.size());
    }

    @Override
    public void onWriteError(@NotNull Exception exception,
                             @NotNull Chunk<? extends PredefinedAnswer> chunk) {
        if (exception instanceof DataIntegrityViolationException) {
            writeStatistics.get().incrementDuplicatesCount(chunk.size());
            log.warn("Duplicate entries detected in chunk of {} items", chunk.size());
        } else {
            writeStatistics.get().incrementWriteErrors(chunk.size());
            log.error("Failed to write chunk of {} items: {}", chunk.size(), exception.getMessage());
        }
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        WriteStatistics stats = writeStatistics.get();
        ExecutionContext context = stepExecution.getExecutionContext();

        context.put(WRITTEN_COUNT, stats.getWrittenCount());
        context.put(DUPLICATES_COUNT, stats.getDuplicatesCount());
        context.put(WRITE_ERRORS, stats.getWriteErrors());

        log.info("Write phase completed. Statistics: {}", stats);
        writeStatistics.remove();

        return stepExecution.getExitStatus();
    }

    @Data
    private static class WriteStatistics {
        private int totalChunks;
        private int writtenCount;
        private int duplicatesCount;
        private int writeErrors;

        public void incrementTotalChunks() {
            totalChunks++;
        }

        public void incrementWrittenCount(int count) {
            writtenCount += count;
        }

        public void incrementDuplicatesCount(int count) {
            duplicatesCount += count;
        }

        public void incrementWriteErrors(int count) {
            writeErrors += count;
        }

        @Override
        public String toString() {
            return String.format("Chunks: %d, Written: %d, Duplicates: %d, Errors: %d",
                    totalChunks, writtenCount, duplicatesCount, writeErrors);
        }
    }
}
