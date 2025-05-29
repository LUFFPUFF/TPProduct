package com.example.domain.api.ans_api_module.generation.exception;

import java.util.List;

public class MLException extends RuntimeException {

    private final int statusCode;
    private List<String> validationErrors;

    public MLException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public MLException(String message, int statusCode, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    public MLException(String message, int statusCode, List<String> validationErrors) {
        super(message);
        this.statusCode = statusCode;
        this.validationErrors = validationErrors;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public List<String> getValidationErrors() {
        return validationErrors;
    }

    @Override
    public String toString() {
        return "ApiException{" +
                "message='" + getMessage() + '\'' +
                ", statusCode=" + statusCode +
                ", validationErrors=" + validationErrors +
                '}';
    }
}
