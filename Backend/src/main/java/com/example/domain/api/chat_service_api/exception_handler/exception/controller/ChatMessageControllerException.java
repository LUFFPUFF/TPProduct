package com.example.domain.api.chat_service_api.exception_handler.exception.controller;

public class ChatMessageControllerException extends RuntimeException {

    public ChatMessageControllerException() {
        super();
    }

    public ChatMessageControllerException(String message) {
        super(message);
    }

    public ChatMessageControllerException(String message, Throwable cause) {
        super(message, cause);
    }

    public ChatMessageControllerException(Throwable cause) {
        super(cause);
    }

    protected ChatMessageControllerException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
