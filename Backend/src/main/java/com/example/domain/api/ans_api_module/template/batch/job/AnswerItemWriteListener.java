package com.example.domain.api.ans_api_module.template.batch.job;


import com.example.database.model.ai_module.PredefinedAnswer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ItemWriteListener;
import org.springframework.batch.item.Chunk;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AnswerItemWriteListener implements ItemWriteListener<PredefinedAnswer> {

    @Override
    public void beforeWrite(Chunk<? extends PredefinedAnswer> chunk) {
        log.debug("Starting to write chunk of {} answers", chunk.size());
    }

    @Override
    public void afterWrite(Chunk<? extends PredefinedAnswer> chunk) {
        log.debug("Successfully wrote {} answers", chunk.size());
    }

    @Override
    public void onWriteError(Exception exception, Chunk<? extends PredefinedAnswer> chunk) {
        log.error("Failed to write {} answers. Reason: {}", chunk.size(), exception.getMessage());
        log.debug("Failed items: {}", chunk.getItems());
    }
}
