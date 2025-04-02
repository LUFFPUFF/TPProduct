package com.example.domain.dto.ans_module.predefined_answer.response;

import java.util.List;

public record AnswerValidationResult(
        boolean isValid,
        List<FieldError> fieldErrors,
        String suggestedFixHint
) {
    public record FieldError(String field, String message) {}
}
