package com.example.domain.api.chat_service_api.exception_handler.exception.service;

public class ChatServiceException extends RuntimeException {

    public ChatServiceException() {
        super();
    }

    public ChatServiceException(String message) {
        super(message);
    }

    public ChatServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    public ChatServiceException(Throwable cause) {
        super(cause);
    }

    protected ChatServiceException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
