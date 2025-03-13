package com.example.domain.api.chat_service_api.exception_handler.exception.controller;

public class ChatControllerException extends RuntimeException {

    public ChatControllerException() {
        super();
    }

    public ChatControllerException(String message) {
        super(message);
    }

    public ChatControllerException(String message, Throwable cause) {
        super(message, cause);
    }

    public ChatControllerException(Throwable cause) {
        super(cause);
    }

    protected ChatControllerException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
