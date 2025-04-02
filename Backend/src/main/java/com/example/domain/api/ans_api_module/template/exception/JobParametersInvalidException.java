package com.example.domain.api.ans_api_module.template.exception;

public class JobParametersInvalidException extends RuntimeException {

    public JobParametersInvalidException() {
        super();
    }

    public JobParametersInvalidException(String message) {
        super(message);
    }

    public JobParametersInvalidException(String message, Throwable cause) {
        super(message, cause);
    }
}
