package com.example.domain.api.chat_service_api.exception_handler.exception.service;

public class WebSocketServiceException extends RuntimeException {

    public WebSocketServiceException() {
        super();
    }

    public WebSocketServiceException(String message) {
        super(message);
    }

    public WebSocketServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    public WebSocketServiceException(Throwable cause) {
        super(cause);
    }

    protected WebSocketServiceException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
