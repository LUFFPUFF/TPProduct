package com.example.domain.api.chat_service_api.service.impl.chat;

import com.example.database.model.chats_messages_module.chat.Chat;
import com.example.database.model.chats_messages_module.chat.ChatStatus;
import com.example.database.model.company_subscription_module.user_roles.user.Role;
import com.example.database.model.company_subscription_module.user_roles.user.User;
import com.example.database.repository.chats_messages_module.ChatRepository;
import com.example.domain.api.chat_service_api.event.chat.ChatClosedEvent;
import com.example.domain.api.chat_service_api.event.chat.ChatStatusChangedEvent;
import com.example.domain.api.chat_service_api.exception_handler.ChatNotFoundException;
import com.example.domain.api.chat_service_api.exception_handler.exception.service.ChatServiceException;
import com.example.domain.api.chat_service_api.mapper.ChatMapper;
import com.example.domain.api.chat_service_api.model.dto.ChatDTO;
import com.example.domain.api.chat_service_api.model.dto.ChatDetailsDTO;
import com.example.domain.api.chat_service_api.service.INotificationService;
import com.example.domain.api.chat_service_api.service.chat.IChatLifecycleService;
import com.example.domain.api.chat_service_api.util.ChatMetricHelper;
import com.example.domain.api.chat_service_api.util.ChatValidationUtil;
import com.example.domain.api.chat_service_api.util.MdcUtil;
import com.example.domain.security.model.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.nio.file.AccessDeniedException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatLifecycleServiceImpl implements IChatLifecycleService {

    private final ChatRepository chatRepository;
    private final ChatMapper chatMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final INotificationService notificationService;
    private final ChatMetricHelper metricHelper;
    private final ChatValidationUtil chatValidationUtil;

    private static final String KEY_COMPANY_ID = "companyId";
    private static final String KEY_CHAT_ID = "chatId";
    private static final String KEY_USER_ID = "userId";
    private static final String KEY_CHAT_STATUS = "newStatus";

    private static final String OPERATION_CLOSE_CHAT = "closeChatByCurrentUser";
    private static final String OPERATION_UPDATE_CHAT = "updateChatStatus";


    @Override
    public ChatDetailsDTO closeChatByCurrentUser(Integer chatId, UserContext userContext) throws AccessDeniedException {
        return MdcUtil.withContext(
                () -> {
                    log.info("User {} attempting to close chat ID {}", userContext.getUserId(), chatId);

                    Chat chat = findChatOrThrow(chatId);
                    chatValidationUtil.ensureChatBelongsToCompany(chat, userContext.getCompanyId(), OPERATION_CLOSE_CHAT);
                    validateUserCanCloseChat(chat, userContext);
                    chatValidationUtil.ensureChatNotClosedOrArchived(chat, "Cannot close chat: already closed or archived.");

                    MDC.put(KEY_COMPANY_ID, metricHelper.getCompanyIdStr(chat.getCompany()));

                    ChatStatus previousStatus = chat.getStatus();
                    User assignedOperator = chat.getUser();

                    chat.setStatus(ChatStatus.CLOSED);
                    chat.setClosedAt(LocalDateTime.now());

                    Chat closedChat = persistChat(chat, OPERATION_CLOSE_CHAT);

                    metricHelper.recordChatDuration(closedChat);

                    if (closedChat.getUser() != null) {
                        notificationService.createNotification(closedChat.getUser(), closedChat, "CHAT_CLOSED", "Чат #" + closedChat.getId() + " был закрыт.");
                    }

                    ChatDTO chatDto = chatMapper.toDto(closedChat);
                    publishChatClosedEvents(closedChat, previousStatus, assignedOperator, chatDto);

                    log.info("Chat ID {} successfully closed by user {}", closedChat.getId(), userContext.getUserId());
                    return chatMapper.toDetailsDto(findChatOrThrow(closedChat.getId()));
                },
                "operation", OPERATION_CLOSE_CHAT,
                KEY_CHAT_ID, chatId,
                KEY_USER_ID, userContext.getUserId()
        );
    }

    @Override
    public ChatDetailsDTO updateChatStatus(Integer chatId, ChatStatus newStatus, UserContext userContext) throws AccessDeniedException {
        return MdcUtil.withContext(
                () -> {
                    log.info("Attempting to update status of chat ID {} to {} by user/process {}",
                            chatId, newStatus, userContext != null ? userContext.getUserId() : "system");

                    Chat chat = findChatOrThrow(chatId);

                    if (userContext != null) {
                        chatValidationUtil.ensureChatBelongsToCompany(chat, userContext.getCompanyId(), OPERATION_UPDATE_CHAT);
                        validateUserCanUpdateStatus(chat, newStatus, userContext);
                    }
                    MDC.put(KEY_COMPANY_ID, metricHelper.getCompanyIdStr(chat.getCompany()));

                    if (chat.getStatus() == newStatus) {
                        log.info("Chat {} already in status {}. No update performed.", chatId, newStatus);
                        return chatMapper.toDetailsDto(chat);
                    }

                    ChatStatus previousStatus = chat.getStatus();

                    chat.setStatus(newStatus);
                    switch (newStatus) {
                        case CLOSED:
                            chat.setClosedAt(LocalDateTime.now());
                            metricHelper.recordChatDuration(chat);
                            break;
                        case ASSIGNED:
                            if (chat.getAssignedAt() == null) chat.setAssignedAt(LocalDateTime.now());
                            metricHelper.recordChatAssignmentTimeIfApplicable(chat);
                            break;
                        case ARCHIVED:
                            if (chat.getClosedAt() == null) chat.setClosedAt(LocalDateTime.now());
                            break;
                        default:
                            break;
                    }

                    Chat updatedChat = persistChat(chat, OPERATION_UPDATE_CHAT);

                    if (previousStatus != updatedChat.getStatus()) {
                        ChatDTO chatDto = chatMapper.toDto(updatedChat);
                        eventPublisher.publishEvent(new ChatStatusChangedEvent(this, updatedChat.getId(), chatDto));
                        log.debug("Published ChatStatusChangedEvent for chat ID {} (status change to {})", updatedChat.getId(), newStatus);

                        if (newStatus == ChatStatus.CLOSED) {
                            User assignedOperator = updatedChat.getUser();
                            publishChatClosedEvents(updatedChat, previousStatus, assignedOperator, chatDto);
                        }
                    }

                    log.info("Chat ID {} status updated to {} by {}", updatedChat.getId(), newStatus, userContext != null ? userContext.getUserId() : "system");
                    return chatMapper.toDetailsDto(findChatOrThrow(updatedChat.getId()));
                },
                "operation", OPERATION_UPDATE_CHAT,
                KEY_CHAT_ID, chatId,
                KEY_CHAT_STATUS, newStatus,
                KEY_USER_ID, userContext != null ? userContext.getUserId() : "system"
        );
    }

    private Chat findChatOrThrow(Integer chatId) {
        return chatRepository.findById(chatId)
                .orElseThrow(() -> new ChatNotFoundException("Chat with ID " + chatId + " not found."));
    }

    private void validateUserCanCloseChat(Chat chat, UserContext userContext) throws AccessDeniedException {
        boolean isAssignedOperator = chat.getUser() != null && Objects.equals(chat.getUser().getId(), userContext.getUserId());
        boolean isManager = userContext.getRoles().stream().anyMatch(role -> role.name().equals("MANAGER"));

        if (!isAssignedOperator && !isManager) {
            log.warn("User {} (roles: {}) attempted to close chat {} but is not assigned operator or manager.",
                    userContext.getUserId(), userContext.getRoles(), chat.getId());
            throw new AccessDeniedException("You do not have permission to close this chat.");
        }
    }

    private void validateUserCanUpdateStatus(Chat chat, ChatStatus newStatus, UserContext userContext) throws AccessDeniedException {
         if (!userContext.getRoles().contains(Role.MANAGER)) {
            throw new AccessDeniedException("You do not have permission to set chat status to " + newStatus);
         }
    }

    private Chat persistChat(Chat chat, String operationName) {
        try {
            Chat savedChat = chatRepository.save(chat);
            log.debug("[{}] Chat entity with ID {} saved/updated.", operationName, savedChat.getId());
            return savedChat;
        } catch (Exception e) {
            metricHelper.incrementChatOperationError(operationName, chat.getCompany(), "ChatPersistenceError");
            log.error("[{}] Error persisting chat for company {}: {}", operationName, metricHelper.getCompanyIdStr(chat.getCompany()), e.getMessage(), e);
            throw new ChatServiceException("Failed to save chat information.", e);
        }
    }

    private void publishChatClosedEvents(Chat closedChat, ChatStatus previousStatus, User assignedOperatorOnClose, ChatDTO chatDto) {
        eventPublisher.publishEvent(new ChatClosedEvent(
                this,
                chatDto,
                assignedOperatorOnClose != null ? assignedOperatorOnClose.getEmail() : null,
                closedChat.getCompany().getId()
        ));

        if (previousStatus != closedChat.getStatus()) {
            eventPublisher.publishEvent(new ChatStatusChangedEvent(this, closedChat.getId(), chatDto));
            log.debug("Published ChatStatusChangedEvent for chat ID {} (closed)", closedChat.getId());
        }
    }
}
