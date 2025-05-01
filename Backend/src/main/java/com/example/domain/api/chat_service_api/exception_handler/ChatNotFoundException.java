package com.example.domain.api.chat_service_api.exception_handler;

public class ChatNotFoundException extends RuntimeException {
    public ChatNotFoundException(String message) {
        super(message);
    }
}
