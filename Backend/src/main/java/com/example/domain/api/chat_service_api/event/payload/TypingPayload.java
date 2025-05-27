package com.example.domain.api.chat_service_api.event.payload;

import com.example.database.model.chats_messages_module.chat.ChatMessageSenderType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TypingPayload {

    private String userId;
    private String userName;
    private ChatMessageSenderType senderType;
    private boolean isTyping;
}
