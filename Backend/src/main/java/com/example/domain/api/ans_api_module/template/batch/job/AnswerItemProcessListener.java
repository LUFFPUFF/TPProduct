package com.example.domain.api.ans_api_module.template.batch.job;

import com.example.database.model.ai_module.PredefinedAnswer;
import com.example.domain.dto.ans_module.predefined_answer.request.PredefinedAnswerUploadDto;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.batch.core.ItemProcessListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AnswerItemProcessListener implements ItemProcessListener<PredefinedAnswerUploadDto, PredefinedAnswer> {

    @Override
    public void beforeProcess(PredefinedAnswerUploadDto item) {
        log.trace("Starting processing for answer: {}", item.getTitle());
    }

    @Override
    public void afterProcess(PredefinedAnswerUploadDto item, @NotNull PredefinedAnswer result) {
        log.trace("Successfully processed answer: {}", item.getTitle());
    }

    @Override
    public void onProcessError(PredefinedAnswerUploadDto item, Exception e) {
        log.error("Error processing answer: {}. Reason: {}", item.getTitle(), e.getMessage());
    }
}
