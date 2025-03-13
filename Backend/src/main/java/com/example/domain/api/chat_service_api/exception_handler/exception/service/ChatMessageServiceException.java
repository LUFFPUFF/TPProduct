package com.example.domain.api.chat_service_api.exception_handler.exception.service;

public class ChatMessageServiceException extends RuntimeException {

    public ChatMessageServiceException() {
        super();
    }

    public ChatMessageServiceException(String message) {
        super(message);
    }

    public ChatMessageServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    public ChatMessageServiceException(Throwable cause) {
        super(cause);
    }

    protected ChatMessageServiceException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
