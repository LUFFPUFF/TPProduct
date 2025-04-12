package com.example.domain.api.ans_api_module.template.dto.response;

import java.util.List;

public record AnswerValidationResult(
        boolean isValid,
        List<FieldError> fieldErrors,
        String suggestedFixHint
) {
    public record FieldError(String field, String message) {}
}
