package com.example.domain.api.chat_service_api.event.chat;

import com.example.domain.api.chat_service_api.event.payload.TypingPayload;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class ChatTypingEvent extends ApplicationEvent {
    private final Integer chatId;
    private final String userId;
    private final TypingPayload typingPayload;

    public ChatTypingEvent(Object source, Integer chatId, String userId, TypingPayload typingPayload) {
        super(source);
        this.chatId = chatId;
        this.userId = userId;
        this.typingPayload = typingPayload;
    }
}
