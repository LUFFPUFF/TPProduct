package com.example.domain.api.chat_service_api.exception_handler.exception;

public class ExternalMessagingException extends RuntimeException {
    public ExternalMessagingException(String message) {
        super(message);
    }

    public ExternalMessagingException(String message, Throwable cause) {
        super(message, cause);
    }

    public ExternalMessagingException(Throwable cause) {
        super(cause);
    }

    public ExternalMessagingException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}
