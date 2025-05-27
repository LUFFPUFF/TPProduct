package com.example.domain.api.chat_service_api.service.chat;

import com.example.database.model.chats_messages_module.chat.ChatStatus;
import com.example.domain.api.chat_service_api.model.dto.ChatDetailsDTO;
import com.example.domain.security.model.UserContext;

import java.nio.file.AccessDeniedException;

public interface IChatLifecycleService {

    ChatDetailsDTO closeChatByCurrentUser(Integer chatId, UserContext userContext) throws AccessDeniedException;

    ChatDetailsDTO updateChatStatus(Integer chatId, ChatStatus newStatus, UserContext userContext) throws AccessDeniedException;
}
