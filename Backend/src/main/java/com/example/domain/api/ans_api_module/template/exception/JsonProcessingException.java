package com.example.domain.api.ans_api_module.template.exception;

public class JsonProcessingException extends RuntimeException {

    public JsonProcessingException() {
    }

    public JsonProcessingException(String message) {
        super(message);
    }

    public JsonProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
