package com.example.domain.api.chat_service_api.util;

import com.example.database.model.chats_messages_module.chat.Chat;
import com.example.database.model.chats_messages_module.chat.ChatMessageSenderType;
import com.example.database.model.company_subscription_module.user_roles.user.User;
import com.example.database.model.crm_module.client.Client;
import com.example.database.repository.chats_messages_module.ChatRepository;
import com.example.domain.api.chat_service_api.exception_handler.ChatNotFoundException;
import com.example.domain.api.chat_service_api.model.rest.mesage.SendMessageRequestDTO;
import com.example.domain.api.chat_service_api.service.IChatMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatInitialMessageHelper {

    private final IChatMessageService chatMessageService;
    private final ChatRepository chatRepository;

    public Chat sendInitialMessageAndUpdateChat(Chat chat, Client clientSender, String content, ChatMessageSenderType senderType) {
        if (!StringUtils.hasText(content)) {
            if (chat.getLastMessageAt() == null) {
                chat.setLastMessageAt(chat.getCreatedAt());
                return chatRepository.save(chat);
            }
            return chat;
        }
        return sendMessageInternal(chat, clientSender.getId(), senderType, content);
    }

    public Chat sendInitialMessageFromOperator(Chat chat, User operatorSender, String content) {
        if (!StringUtils.hasText(content)) {
            return chat;
        }
        return sendMessageInternal(chat, operatorSender.getId(), ChatMessageSenderType.OPERATOR, content);
    }

    private Chat sendMessageInternal(Chat chat, Integer senderId, ChatMessageSenderType senderType, String content) {
        SendMessageRequestDTO messageRequest = new SendMessageRequestDTO();
        messageRequest.setChatId(chat.getId());
        messageRequest.setContent(content);
        messageRequest.setSenderId(senderId);
        messageRequest.setSenderType(senderType);

        chatMessageService.processAndSaveMessage(
                messageRequest,
                senderId,
                senderType
        );

        return chatRepository.findById(chat.getId())
                .orElseThrow(() -> {
                    log.error("CRITICAL: Chat with ID {} disappeared after saving initial message.", chat.getId());
                    return new ChatNotFoundException("Chat consistency error: Chat not found after saving initial message: " + chat.getId());
                });
    }
}
