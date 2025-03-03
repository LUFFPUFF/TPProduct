package com.example.domain.exception_handler.chat_module;

public class ChatAttachmentException extends RuntimeException{

    public ChatAttachmentException(String message) {
        super(message);
    }

    public ChatAttachmentException(String message, Throwable cause) {
        super(message, cause);
    }
}
