package com.example.domain.api.ans_api_module.template.exception;

public class InvalidTagException extends RuntimeException {
    public InvalidTagException(String message) {
        super(message);
    }

    public InvalidTagException(String message, Throwable cause) {
        super(message, cause);
    }
}
