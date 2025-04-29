package com.example.domain.api.ans_api_module.template.exception;

import com.example.database.model.ai_module.PredefinedAnswer;
import lombok.Getter;

import java.util.List;

@Getter
public class BatchWriteException extends RuntimeException {
    private final List<? extends PredefinedAnswer> failedItems;

    public BatchWriteException(String message, Throwable cause, List<? extends PredefinedAnswer> failedItems) {
        super(message, cause);
        this.failedItems = failedItems;
    }

}
