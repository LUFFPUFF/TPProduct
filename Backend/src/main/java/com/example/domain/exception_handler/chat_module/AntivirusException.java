package com.example.domain.exception_handler.chat_module;

public class AntivirusException extends RuntimeException {
    public AntivirusException(String message, Throwable cause) {
        super(message, cause);
    }
}
