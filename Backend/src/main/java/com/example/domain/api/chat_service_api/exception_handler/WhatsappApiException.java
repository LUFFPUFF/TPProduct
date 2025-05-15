package com.example.domain.api.chat_service_api.exception_handler;

public class WhatsappApiException extends RuntimeException {
    public WhatsappApiException(String message) { super(message); }
    public WhatsappApiException(String message, Throwable cause) { super(message, cause); }
}
