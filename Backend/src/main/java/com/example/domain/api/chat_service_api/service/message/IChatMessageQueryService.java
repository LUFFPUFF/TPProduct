package com.example.domain.api.chat_service_api.service.message;

import com.example.database.model.chats_messages_module.message.ChatMessage;
import com.example.domain.api.chat_service_api.model.dto.MessageDto;
import com.example.domain.security.model.UserContext;

import java.nio.file.AccessDeniedException;
import java.util.List;
import java.util.Optional;

public interface IChatMessageQueryService {

    List<MessageDto> getMessagesByChatId(Integer chatId, UserContext userContext) throws AccessDeniedException;

    Optional<ChatMessage> findFirstMessageEntityByChatId(Integer chatId, UserContext userContext) throws AccessDeniedException;

    List<MessageDto> getRecentClientMessages(Integer chatId, int limit, Integer excludeMessageId, UserContext userContext) throws AccessDeniedException;

    Optional<ChatMessage> findMessageEntityById(Integer messageId, UserContext userContext) throws AccessDeniedException;
}
