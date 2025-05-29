package com.example.domain.api.chat_service_api.service.chat;

import com.example.database.model.chats_messages_module.chat.Chat;
import com.example.database.model.chats_messages_module.chat.ChatChannel;
import com.example.database.model.chats_messages_module.chat.ChatStatus;
import com.example.database.model.chats_messages_module.message.ChatMessage;
import com.example.domain.api.chat_service_api.model.dto.ChatDTO;
import com.example.domain.api.chat_service_api.model.dto.ChatDetailsDTO;
import com.example.domain.security.model.UserContext;

import java.nio.file.AccessDeniedException;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface IChatQueryService {

    ChatDetailsDTO getChatDetails(Integer chatId, UserContext userContext) throws AccessDeniedException;

    Optional<Chat> findOpenChatEntityByClientAndChannel(Integer clientId, ChatChannel channel);

    Optional<Chat> findOpenChatEntityByClientAndChannelAndExternalId(Integer clientId, ChatChannel channel, String externalChatId);

    Optional<ChatMessage> findFirstMessageEntityByChatId(Integer chatId, UserContext userContext) throws AccessDeniedException;

    Optional<Chat> findChatEntityById(Integer chatId);

    List<ChatDTO> getMyViewableChats(Set<ChatStatus> statuses, UserContext userContext) throws AccessDeniedException;

    List<ChatDTO> getMyAssignedChats(Set<ChatStatus> statuses, UserContext userContext);

    List<ChatDTO> getOperatorChats(Integer operatorId, Set<ChatStatus> statuses, UserContext userContext) throws AccessDeniedException;

    List<ChatDTO> getClientChats(Integer clientId, UserContext userContext) throws AccessDeniedException;

    List<ChatDTO> getMyCompanyPendingOperatorChats(UserContext userContext) throws AccessDeniedException;

    Optional<Chat> findChatEntityByExternalId(Integer companyId, Integer clientId, ChatChannel channel, String externalChatId);
}
