package com.example.domain.api.chat_service_api.util;

import com.example.database.model.chats_messages_module.chat.Chat;
import com.example.database.model.chats_messages_module.chat.ChatMessageSenderType;
import com.example.database.model.chats_messages_module.message.ChatMessage;
import com.example.database.model.chats_messages_module.message.MessageStatus;
import com.example.database.model.company_subscription_module.user_roles.user.User;
import com.example.database.model.crm_module.client.Client;
import com.example.domain.api.chat_service_api.model.rest.mesage.SendMessageRequestDTO;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class ChatMessageFactory {

    private static final int MAX_CONTENT_LENGTH = 4000;
    private static final String TRUNCATE_INDICATOR = "...";

    public ChatMessage createMessageEntity(SendMessageRequestDTO request, Chat chat,
                                           User senderOperator, Client senderClient,
                                           ChatMessageSenderType senderType) {
        ChatMessage message = new ChatMessage();
        message.setChat(chat);
        message.setSenderOperator(senderOperator);
        message.setSenderClient(senderClient);
        message.setSenderType(senderType);
        message.setContent(truncateContent(request.getContent()));
        message.setExternalMessageId(request.getExternalMessageId());
        message.setReplyToExternalMessageId(request.getReplyToExternalMessageId());
        message.setSentAt(LocalDateTime.now());
        message.setStatus(MessageStatus.SENT);
        return message;
    }

    private String truncateContent(String content) {
        if (content == null) {
            return null;
        }
        if (content.length() <= MAX_CONTENT_LENGTH) {
            return content;
        }
        String truncated = content.substring(0, MAX_CONTENT_LENGTH - TRUNCATE_INDICATOR.length());
        return truncated + TRUNCATE_INDICATOR;
    }
}
