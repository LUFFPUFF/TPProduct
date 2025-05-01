package com.example.domain.api.ans_api_module.exception;

public class AutoResponderException extends RuntimeException {
    public AutoResponderException(String message) {
        super(message);
    }

    public AutoResponderException(String message, Throwable cause) {
        super(message, cause);
    }

    public AutoResponderException(Throwable cause) {
        super(cause);
    }

    public AutoResponderException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}
