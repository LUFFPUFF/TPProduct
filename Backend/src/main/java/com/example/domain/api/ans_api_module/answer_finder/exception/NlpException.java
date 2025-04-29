package com.example.domain.api.ans_api_module.answer_finder.exception;

public class NlpException extends RuntimeException {

    public NlpException(String message) {
        super(message);
    }

    public NlpException(String message, Throwable cause) {
        super(message, cause);
    }
}
