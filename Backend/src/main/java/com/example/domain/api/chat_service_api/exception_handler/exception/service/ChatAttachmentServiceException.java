package com.example.domain.api.chat_service_api.exception_handler.exception.service;

public class ChatAttachmentServiceException extends RuntimeException{

    public ChatAttachmentServiceException(String message) {
        super(message);
    }

    public ChatAttachmentServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
