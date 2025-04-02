package com.example.domain.api.ans_api_module.template.exception;

import com.example.domain.dto.ans_module.predefined_answer.request.PredefinedAnswerUploadDto;
import lombok.Getter;

@Getter
public class InvalidAnswerDataException extends Exception {
    private final PredefinedAnswerUploadDto invalidData;

    public InvalidAnswerDataException(String message, Throwable cause, PredefinedAnswerUploadDto invalidData) {
        super(message, cause);
        this.invalidData = invalidData;
    }

}
