package com.example.domain.api.chat_service_api.service.impl.chat;

import com.example.database.model.chats_messages_module.chat.Chat;
import com.example.database.model.chats_messages_module.chat.ChatChannel;
import com.example.database.model.chats_messages_module.chat.ChatMessageSenderType;
import com.example.database.model.chats_messages_module.chat.ChatStatus;
import com.example.domain.api.chat_service_api.exception_handler.ChatNotFoundException;
import com.example.domain.api.chat_service_api.exception_handler.exception.service.ChatServiceException;
import com.example.domain.api.chat_service_api.model.dto.ChatDTO;
import com.example.domain.api.chat_service_api.model.dto.ChatDetailsDTO;
import com.example.domain.api.chat_service_api.model.dto.MessageDto;
import com.example.domain.api.chat_service_api.model.rest.chat.AssignChatRequestDTO;
import com.example.domain.api.chat_service_api.model.rest.chat.CreateChatRequestDTO;
import com.example.domain.api.chat_service_api.model.rest.mesage.SendMessageRequestDTO;
import com.example.domain.api.chat_service_api.service.*;

import com.example.domain.api.chat_service_api.service.chat.*;
import com.example.domain.api.statistics_module.aop.annotation.Counter;
import com.example.domain.api.statistics_module.aop.annotation.MeteredOperation;
import com.example.domain.api.statistics_module.aop.annotation.Tag;
import com.example.domain.security.model.UserContext;
import com.example.domain.security.util.UserContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.AccessDeniedException;
import java.util.*;

@Service("chatServiceFacade")
@RequiredArgsConstructor
@Slf4j
public class ChatServiceImpl implements IChatService {

    private final IChatCreationService chatCreationService;
    private final IChatAssignmentService chatAssignmentService;
    private final IChatLifecycleService chatLifecycleService;
    private final IChatQueryService chatQueryService;
    private final IChatMessageService chatMessageService;

    @Override
    public ChatDetailsDTO createChat(CreateChatRequestDTO createRequest) {
        return chatCreationService.createChat(createRequest);
    }

    @Override
    public ChatDetailsDTO createChatWithOperatorFromUI(CreateChatRequestDTO createRequest) throws AccessDeniedException {
        UserContext userContext = UserContextHolder.getRequiredContext();
        return chatCreationService.createChatFromOperatorUI(createRequest, userContext);
    }

    @Override
    public ChatDetailsDTO requestOperatorEscalation(Integer chatId, Integer clientId) {
        return chatAssignmentService.escalateChatToOperator(chatId, clientId);
    }

    @Override
    public void linkOperatorToChat(Integer chatId, Integer operatorId) throws AccessDeniedException {
        UserContext userContext = UserContextHolder.getRequiredContext();
        chatAssignmentService.linkOperatorToChat(chatId, operatorId, userContext);
    }

    @Override
    public Optional<Chat> findOpenChatByClientAndChannel(Integer clientId, ChatChannel channel) {
        return chatQueryService.findOpenChatEntityByClientAndChannel(clientId, channel);
    }

    @Override
    public Optional<Chat> findOpenChatByClientAndChannelAndExternalId(Integer clientId, ChatChannel channel, String externalChatId) {
        return chatQueryService.findOpenChatEntityByClientAndChannelAndExternalId(clientId, channel, externalChatId);
    }

    @Override
    public Optional<Chat> findChatEntityById(Integer chatId) {
        return chatQueryService.findChatEntityById(chatId);
    }

    @Override
    public ChatDetailsDTO assignChat(AssignChatRequestDTO assignRequest) throws AccessDeniedException {
        UserContext userContext = UserContextHolder.getRequiredContext();
        return chatAssignmentService.assignChatToOperator(assignRequest, userContext);
    }

    @Override
    public ChatDetailsDTO closeChatByCurrentUser(Integer chatId) throws AccessDeniedException {
        UserContext userContext = UserContextHolder.getRequiredContext();
        return chatLifecycleService.closeChatByCurrentUser(chatId, userContext);
    }

    @Override
    public ChatDetailsDTO getChatDetails(Integer chatId) throws AccessDeniedException {
        UserContext userContext = UserContextHolder.getRequiredContext();
        return chatQueryService.getChatDetails(chatId, userContext);
    }

    @Override
    public List<ChatDTO> getMyChats(Set<ChatStatus> statuses) throws AccessDeniedException {
        UserContext userContext = UserContextHolder.getRequiredContext();
        return chatQueryService.getMyViewableChats(statuses, userContext);
    }

    @Override
    public List<ChatDTO> getChatsForCurrentUser(Set<ChatStatus> statuses) {
        UserContext userContext = UserContextHolder.getRequiredContext();
        return chatQueryService.getMyAssignedChats(statuses, userContext);
    }

    @Override
    public List<ChatDTO> getOperatorChats(Integer operatorId, Set<ChatStatus> statuses) throws AccessDeniedException {
        UserContext userContext = UserContextHolder.getRequiredContext();
        return chatQueryService.getOperatorChats(operatorId, statuses, userContext);
    }

    @Override
    public List<ChatDTO> getClientChats(Integer clientId) throws AccessDeniedException {
        UserContext userContext = UserContextHolder.getRequiredContext();
        return chatQueryService.getClientChats(clientId, userContext);
    }

    @Override
    public List<ChatDTO> getMyCompanyWaitingChats() throws AccessDeniedException {
        UserContext userContext = UserContextHolder.getRequiredContext();
        return chatQueryService.getMyCompanyPendingOperatorChats(userContext);
    }

    @Override
    public void updateChatStatus(Integer chatId, ChatStatus newStatus) throws AccessDeniedException {
        UserContext userContext = UserContextHolder.getRequiredContext();
        chatLifecycleService.updateChatStatus(chatId, newStatus, userContext);
    }

    @Override
    public Optional<Chat> findChatByExternalId(Integer companyId, Integer clientId, ChatChannel channel, String externalChatId) {
        return chatQueryService.findChatEntityByExternalId(companyId, clientId, channel, externalChatId);
    }

    @Override
    @Transactional
    @MeteredOperation(prefix = "chat_app_",
            counters = @Counter(name = "operator_messages_sent_total",
                    tags = { @Tag(key="company_id", valueSpEL="#chatEntity.company?.id?.toString() ?: 'unknown'"),
                            @Tag(key="operator_id", valueSpEL="#userContext.userId?.toString() ?: 'unknown'"),
                            @Tag(key="channel", valueSpEL="#chatEntity.chatChannel?.name() ?: 'UNKNOWN'")})
    )
    public MessageDto sendOperatorMessage(Integer chatId, String content) {
        UserContext userContext = UserContextHolder.getRequiredContext();

        SendMessageRequestDTO messageRequest = new SendMessageRequestDTO();
        messageRequest.setChatId(chatId);
        messageRequest.setContent(content);
        messageRequest.setSenderId(userContext.getUserId());
        messageRequest.setSenderType(ChatMessageSenderType.OPERATOR);

        Chat chatEntity = chatQueryService.findChatEntityById(chatId)
                .orElseThrow(() -> {
                    log.warn("Chat with ID {} not found for message sending by operator {}.", chatId, userContext.getUserId());
                    return new ChatNotFoundException("Chat with ID " + chatId + " not found.");
                });

        if (chatEntity.getCompany() == null || !Objects.equals(chatEntity.getCompany().getId(), userContext.getCompanyId())) {
            log.warn("Operator {} from company {} attempted to send message to chat {} of company {}",
                    userContext.getUserId(), userContext.getCompanyId(), chatId,
                    chatEntity.getCompany() != null ? chatEntity.getCompany().getId() : "null");
            throw new ChatServiceException("Access denied: Cannot send message to a chat from a different company.");
        }

        if (chatEntity.getStatus() == ChatStatus.PENDING_AUTO_RESPONDER ||
                chatEntity.getStatus() == ChatStatus.CLOSED ||
                chatEntity.getStatus() == ChatStatus.ARCHIVED) {
            throw new ChatServiceException("Cannot send message to chat with status: " + chatEntity.getStatus() +
                    ". Allowed statuses are typically ASSIGNED, IN_PROGRESS, PENDING_OPERATOR.");
        }
        return chatMessageService.processAndSaveMessage(messageRequest,
                userContext.getUserId(),
                ChatMessageSenderType.OPERATOR
        );
    }

    @Override
    public ChatDetailsDTO createTestChatForCurrentUser() throws AccessDeniedException {
        UserContext userContext = UserContextHolder.getRequiredContext();
        return chatCreationService.createTestChatForCurrentUser(userContext);
    }

    @Override
    public void markClientMessagesAsReadByCurrentUser(Integer chatId, Collection<Integer> messageIds) throws AccessDeniedException {
        UserContext userContext = UserContextHolder.getRequiredContext();
        if (messageIds == null || messageIds.isEmpty()) {
            log.debug("No message IDs provided to mark as read for chat {}. Skipping.", chatId);
            return;
        }

        Chat chatEntity = chatQueryService.findChatEntityById(chatId)
                .orElseThrow(() -> {
                    log.warn("User {} attempted to mark messages as read for non-existent chat {}.", userContext.getUserId(), chatId);
                    return new ChatNotFoundException("Chat with ID " + chatId + " not found.");
                });

        if (chatEntity.getCompany() == null || !Objects.equals(chatEntity.getCompany().getId(), userContext.getCompanyId())) {
            log.warn("User {} from company {} attempted to mark messages as read in chat {} of company {}.",
                    userContext.getUserId(), userContext.getCompanyId(), chatId,
                    chatEntity.getCompany() != null ? chatEntity.getCompany().getId() : "null");
            throw new AccessDeniedException("Access denied: Cannot mark messages as read in a chat from a different company.");
        }

        chatMessageService.markClientMessagesAsRead(chatId, userContext.getUserId(), messageIds, userContext);
    }


}
