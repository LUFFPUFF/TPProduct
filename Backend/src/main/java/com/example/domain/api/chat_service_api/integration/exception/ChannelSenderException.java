package com.example.domain.api.chat_service_api.integration.exception;

public class ChannelSenderException extends RuntimeException {
    public ChannelSenderException(String message) {
        super(message);
    }

    public ChannelSenderException(String message, Throwable cause) {
        super(message, cause);
    }
}
