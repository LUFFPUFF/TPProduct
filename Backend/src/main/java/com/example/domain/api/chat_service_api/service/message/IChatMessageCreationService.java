package com.example.domain.api.chat_service_api.service.message;

import com.example.database.model.chats_messages_module.chat.ChatMessageSenderType;
import com.example.domain.api.chat_service_api.model.dto.MessageDto;
import com.example.domain.api.chat_service_api.model.rest.mesage.SendMessageRequestDTO;

public interface IChatMessageCreationService {

    MessageDto processAndSaveMessage(SendMessageRequestDTO messageRequest, Integer senderId, ChatMessageSenderType senderType);
}
