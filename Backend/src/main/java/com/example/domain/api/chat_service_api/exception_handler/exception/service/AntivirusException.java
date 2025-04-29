package com.example.domain.api.chat_service_api.exception_handler.exception.service;

public class AntivirusException extends RuntimeException {

    public AntivirusException() {
        super();
    }

    public AntivirusException(String message) {
        super(message);
    }

    public AntivirusException(String message, Throwable cause) {
        super(message, cause);
    }

    public AntivirusException(Throwable cause) {
        super(cause);
    }

    protected AntivirusException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
