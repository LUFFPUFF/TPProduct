package com.example.domain.api.ans_api_module.template.exception;

public class InvalidFileException extends RuntimeException {

    public InvalidFileException() {
    }

    public InvalidFileException(String message) {
        super(message);
    }

    public InvalidFileException(String message, Throwable cause) {
        super(message, cause);
    }
}
