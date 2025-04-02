package com.example.domain.api.ans_api_module.template.exception;

public class InvalidFileFormatException extends RuntimeException {

    public InvalidFileFormatException() {
    }

    public InvalidFileFormatException(String message) {
        super(message);
    }

    public InvalidFileFormatException(String message, Throwable cause) {
        super(message, cause);
    }
}
