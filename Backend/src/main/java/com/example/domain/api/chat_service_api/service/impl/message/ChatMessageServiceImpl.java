package com.example.domain.api.chat_service_api.service.impl.message;

import com.example.database.model.chats_messages_module.chat.Chat;
import com.example.database.model.chats_messages_module.chat.ChatMessageSenderType;
import com.example.database.model.chats_messages_module.message.ChatMessage;
import com.example.database.model.chats_messages_module.message.MessageStatus;
import com.example.database.repository.chats_messages_module.ChatRepository;
import com.example.domain.api.chat_service_api.model.dto.MessageDto;
import com.example.domain.api.chat_service_api.model.rest.mesage.SendMessageRequestDTO;
import com.example.domain.api.chat_service_api.service.IChatMessageService;

import com.example.domain.api.chat_service_api.service.message.IChatMessageCreationService;
import com.example.domain.api.chat_service_api.service.message.IChatMessageQueryService;
import com.example.domain.api.chat_service_api.service.message.IChatMessageStatusService;
import com.example.domain.security.model.UserContext;
import com.example.domain.security.util.UserContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.AccessDeniedException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Service("chatMessageServiceFacade")
@RequiredArgsConstructor
@Slf4j
public class ChatMessageServiceImpl implements IChatMessageService {

    private final ChatRepository chatRepository;
    private final IChatMessageCreationService messageCreationService;
    private final IChatMessageStatusService messageStatusService;
    private final IChatMessageQueryService messageQueryService;

    @Override
    public MessageDto processAndSaveMessage(SendMessageRequestDTO messageRequest, Integer senderId, ChatMessageSenderType senderType) {
        return messageCreationService.processAndSaveMessage(messageRequest, senderId, senderType);
    }

    @Override
    public List<MessageDto> getMessagesByChatId(Integer chatId) throws AccessDeniedException {
        UserContext userContext = UserContextHolder.getRequiredContext();
        return messageQueryService.getMessagesByChatId(chatId, userContext);
    }

    @Override
    public MessageDto updateMessageStatus(Integer messageId, MessageStatus newStatus, UserContext userContext) throws AccessDeniedException {
        return messageStatusService.updateMessageStatus(messageId, newStatus, userContext);
    }

    @Override
    public int markClientMessagesAsRead(Integer chatId, Integer operatorId, Collection<Integer> messageIds, UserContext userContext) throws AccessDeniedException {
        return messageStatusService.markClientMessagesAsRead(chatId, operatorId, messageIds, userContext);
    }

    @Override
    public Optional<ChatMessage> findFirstMessageByChatId(Integer chatId) throws AccessDeniedException {
        UserContext userContext = UserContextHolder.getRequiredContext();
        return messageQueryService.findFirstMessageEntityByChatId(chatId, userContext);
    }

    @Override
    public int updateOperatorMessageStatusByExternalId(Integer chatId, String externalMessageId, MessageStatus newStatus) {
        return messageStatusService.updateOperatorMessageStatusByExternalId(chatId, externalMessageId, newStatus);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Chat> findChatEntityById(Integer chatId) {
        return chatRepository.findById(chatId);
    }

    @Override
    @Deprecated
    public ChatMessage saveIncomingMessageFromExternal(Integer chatId, ChatMessageSenderType senderType, Integer senderId, String content, String externalMessageId, String replyToExternalMessageId) {
        throw new UnsupportedOperationException("Deprecated and requires refactoring to align with DTO-based responses or use processAndSaveMessage.");
    }
}
