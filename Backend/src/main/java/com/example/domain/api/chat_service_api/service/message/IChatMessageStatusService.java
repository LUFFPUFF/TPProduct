package com.example.domain.api.chat_service_api.service.message;

import com.example.database.model.chats_messages_module.message.MessageStatus;
import com.example.domain.api.chat_service_api.model.dto.MessageDto;
import com.example.domain.security.model.UserContext;

import java.nio.file.AccessDeniedException;
import java.util.Collection;

public interface IChatMessageStatusService {

    MessageDto updateMessageStatus(Integer messageId, MessageStatus newStatus, UserContext userContext) throws AccessDeniedException;

    int markClientMessagesAsRead(Integer chatId, Integer operatorId, Collection<Integer> messageIds, UserContext userContext) throws AccessDeniedException;

    int updateOperatorMessageStatusByExternalId(Integer chatId, String externalMessageId, MessageStatus newStatus);
}
