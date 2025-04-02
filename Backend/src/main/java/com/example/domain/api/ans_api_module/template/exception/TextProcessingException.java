package com.example.domain.api.ans_api_module.template.exception;

public class TextProcessingException extends RuntimeException {

    public TextProcessingException() {
    }

    public TextProcessingException(String message) {
        super(message);
    }

    public TextProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
