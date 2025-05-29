package com.example.domain.api.chat_service_api.service.impl.chat;

import com.example.database.model.chats_messages_module.chat.Chat;
import com.example.database.model.chats_messages_module.chat.ChatStatus;
import com.example.database.model.company_subscription_module.company.Company;
import com.example.database.model.company_subscription_module.user_roles.user.User;
import com.example.database.repository.chats_messages_module.ChatRepository;
import com.example.domain.api.ans_api_module.event.AutoResponderEscalationEvent;
import com.example.domain.api.ans_api_module.service.IAutoResponderService;
import com.example.domain.api.chat_service_api.event.chat.ChatAssignedToOperatorEvent;
import com.example.domain.api.chat_service_api.event.chat.ChatEscalatedToOperatorEvent;
import com.example.domain.api.chat_service_api.event.chat.ChatStatusChangedEvent;
import com.example.domain.api.chat_service_api.event.chat.NewPendingChatEvent;
import com.example.domain.api.chat_service_api.exception_handler.ChatNotFoundException;
import com.example.domain.api.chat_service_api.exception_handler.ResourceNotFoundException;
import com.example.domain.api.chat_service_api.exception_handler.exception.service.ChatServiceException;
import com.example.domain.api.chat_service_api.mapper.ChatMapper;
import com.example.domain.api.chat_service_api.model.dto.ChatDTO;
import com.example.domain.api.chat_service_api.model.dto.ChatDetailsDTO;
import com.example.domain.api.chat_service_api.model.rest.chat.AssignChatRequestDTO;
import com.example.domain.api.chat_service_api.service.IAssignmentStrategyService;
import com.example.domain.api.chat_service_api.service.INotificationService;
import com.example.domain.api.company_module.service.IUserService;
import com.example.domain.api.chat_service_api.service.chat.IChatAssignmentService;
import com.example.domain.api.chat_service_api.service.impl.LeastBusyAssignmentService;
import com.example.domain.api.chat_service_api.util.ChatMetricHelper;
import com.example.domain.api.chat_service_api.util.ChatValidationUtil;
import com.example.domain.api.chat_service_api.util.MdcUtil;
import com.example.domain.api.statistics_module.aop.annotation.Counter;
import com.example.domain.api.statistics_module.aop.annotation.MeteredOperation;
import com.example.domain.api.statistics_module.aop.annotation.Tag;
import com.example.domain.security.model.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.AccessDeniedException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatAssignmentServiceImpl implements IChatAssignmentService {

    private final ChatRepository chatRepository;
    private final IUserService userService;
    private final IAssignmentStrategyService assignmentStrategyService;
    private final INotificationService notificationService;
    private final IAutoResponderService autoResponderService;
    private final ChatMapper chatMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final ChatMetricHelper metricHelper;
    private final ChatValidationUtil chatValidationUtil;

    private static final String KEY_COMPANY_ID_MDC = "companyIdMdc";
    private static final String KEY_CHAT_ID = "chatId";
    private static final String KEY_CHAT_ID_MDC = "chatIdMdc";
    private static final String KEY_CLIENT_ID = "clientId";
    private static final String KEY_OPERATOR_ID = "operatorId";
    private static final String KEY_USER_ID = "userId";

    private static final String OPERATION_ASSIGN_CHAT = "assignChatToOperator";
    private static final String OPERATION_ESCALATE_CHAT = "escalateChatToOperator";
    private static final String OPERATION_LINK_OPERATOR = "linkOperatorToChat";
    private static final String OPERATION_HANDLE_AUTO_ESCALATION = "handleAutoResponderEscalation";
    private static final String OPERATION_ESCALATE_CHAT_INTERNAL = "escalateChatToOperatorInternal";

    private static final int DEFAULT_MAX_CONCURRENT_CHATS_FOR_EXPLICIT_ASSIGN = 10;


    @Override
    @Transactional
    @MeteredOperation(
            counters = @Counter(name = "chat_app_chats_assigned_total",
                    tags = {@Tag(key = "company_id", valueSpEL = "#result.companyId ?: 'unknown'"),
                            @Tag(key = "channel", valueSpEL = "#result.chatChannel?.name() ?: 'UNKNOWN'"),
                            @Tag(key = "auto_assigned", valueSpEL = "#assignRequest.operatorId == null ? 'true' : 'false'")})
    )
    public ChatDetailsDTO assignChatToOperator(AssignChatRequestDTO assignRequest, UserContext userContext) throws AccessDeniedException {
        return MdcUtil.withContext(
                () -> {
                    log.info("Attempting to assign chat ID {} by user {}. Requested operator ID: {}",
                            assignRequest.getChatId(), userContext.getUserId(), assignRequest.getOperatorId());

                    Chat chat = findChatOrThrow(assignRequest.getChatId());
                    chatValidationUtil.ensureChatBelongsToCompany(chat, userContext.getCompanyId(), OPERATION_ASSIGN_CHAT);
                    chatValidationUtil.ensureChatIsInStatus(chat, ChatStatus.PENDING_OPERATOR, "Cannot assign chat: not in PENDING_OPERATOR status.");

                    MDC.put(KEY_COMPANY_ID_MDC, metricHelper.getCompanyIdStr(chat.getCompany()));
                    MDC.put(KEY_CHAT_ID_MDC, String.valueOf(chat.getId()));

                    User operatorToAssign = determineOperatorForAssignment(chat, assignRequest.getOperatorId(), userContext.getCompanyId());

                    if (isAlreadyAssignedToOperator(chat, operatorToAssign)) {
                        log.info("Chat {} already assigned to operator {}. No changes made.", chat.getId(), operatorToAssign.getId());
                        return chatMapper.toDetailsDto(chat);
                    }

                    return performAssignment(chat, operatorToAssign, OPERATION_ASSIGN_CHAT);
                },
                "operation", OPERATION_ASSIGN_CHAT,
                KEY_CHAT_ID, assignRequest.getChatId(),
                "requestedOperatorId", assignRequest.getOperatorId(),
                KEY_USER_ID, userContext.getUserId()
        );
    }

    @Override
    @Transactional
    @MeteredOperation(
            counters = @Counter(name = "chat_app_chats_escalated_total",
                    tags = { @Tag(key = "company_id", valueSpEL = "#result.companyId ?: 'unknown'"),
                            @Tag(key = "channel", valueSpEL = "#result.chatChannel?.name() ?: 'UNKNOWN'")})
    )
    public ChatDetailsDTO escalateChatToOperator(Integer chatId, Integer clientId) {
        return MdcUtil.withContext(
                () -> {
                    log.info("Client {} requesting operator escalation for chat ID {}", clientId, chatId);

                    Chat chat = findChatOrThrow(chatId);
                    chatValidationUtil.ensureClientOwnsChat(chat, clientId, OPERATION_ESCALATE_CHAT);
                    chatValidationUtil.ensureChatNotClosedOrArchived(chat, "Cannot escalate chat: already closed or archived.");

                    MDC.put(KEY_COMPANY_ID_MDC, metricHelper.getCompanyIdStr(chat.getCompany()));
                    MDC.put(KEY_CHAT_ID_MDC, String.valueOf(chat.getId()));

                    autoResponderService.stopForChat(chatId);
                    log.info("Auto-responder stopped for chat ID {} due to escalation.", chatId);

                    User previouslyAssignedOperator = chat.getUser();
                    ChatStatus previousStatus = chat.getStatus();

                    Optional<User> operatorOpt = assignmentStrategyService.findOperatorForChat(chat);
                    Chat updatedChat;

                    if (operatorOpt.isPresent()) {
                        User newOperator = operatorOpt.get();
                        log.info("Assigning escalated chat {} to operator {}", chatId, newOperator.getId());
                        updatedChat = updateChatAssignment(chat, newOperator, ChatStatus.ASSIGNED, LocalDateTime.now());
                        notificationService.createNotification(newOperator, updatedChat,
                                "CHAT_ASSIGNED_ESCALATION", "Вам назначен чат #" + updatedChat.getId() + " после эскалации клиентом.");
                    } else {
                        log.warn("No operator found for escalated chat {}. Setting status to PENDING_OPERATOR.", chatId);
                        updatedChat = updateChatStatus(chat, ChatStatus.PENDING_OPERATOR);
                    }

                    ChatDetailsDTO chatDetailsDto = finalizeAndPublishEscalationEvents(
                            updatedChat, previousStatus, previouslyAssignedOperator, operatorOpt.orElse(null));

                    metricHelper.recordChatAssignmentTimeIfApplicable(updatedChat);
                    return chatDetailsDto;
                },
                "operation", OPERATION_ESCALATE_CHAT,
                KEY_CHAT_ID, chatId,
                KEY_CLIENT_ID, clientId
        );
    }

    @Override
    @Transactional
    public void linkOperatorToChat(Integer chatId, Integer operatorId, UserContext userContext) throws AccessDeniedException {
        MdcUtil.withContext(
                () -> {
                    log.info("User {} attempting to link operator {} to chat {}", userContext.getUserId(), operatorId, chatId);

                    Chat chat = findChatOrThrow(chatId);
                    User operator = findUserOrThrow(operatorId);
                    MDC.put(KEY_COMPANY_ID_MDC, metricHelper.getCompanyIdStr(chat.getCompany()));
                    MDC.put(KEY_CHAT_ID_MDC, String.valueOf(chat.getId()));


                    chatValidationUtil.ensureEntitiesBelongToSameCompany(
                            "Operator-Chat linking failed: ",
                            chat.getCompany(), operator.getCompany(),
                            userService.findUserCompanyOrThrow(userContext.getUserId())
                    );

                    if (isAlreadyAssignedToOperator(chat, operator)) {
                        log.info("Chat {} already linked to operator {}. No changes made.", chatId, operatorId);
                        return null;
                    }

                    ChatStatus previousStatus = chat.getStatus();
                    User previousOperator = chat.getUser();

                    chat.setUser(operator);
                    boolean assignedNow = false;
                    if (chat.getStatus() == ChatStatus.PENDING_OPERATOR ||
                            chat.getStatus() == ChatStatus.PENDING_AUTO_RESPONDER ||
                            (previousOperator != null && !Objects.equals(previousOperator.getId(), operator.getId()))) {

                        if (chat.getStatus() != ChatStatus.ASSIGNED && chat.getStatus() != ChatStatus.IN_PROGRESS) {
                            chat.setStatus(ChatStatus.ASSIGNED);
                        }
                        if (chat.getAssignedAt() == null || (previousOperator !=null && !previousOperator.getId().equals(operator.getId())) ) {
                            chat.setAssignedAt(LocalDateTime.now());
                            assignedNow = true;
                        }
                    }

                    Chat updatedChat = persistChat(chat, OPERATION_LINK_OPERATOR, chat.getCompany());

                    metricHelper.incrementChatOperatorLinked(updatedChat.getCompany(), updatedChat.getChatChannel());
                    if (assignedNow) {
                        metricHelper.recordChatAssignmentTimeIfApplicable(updatedChat);
                    }

                    publishAssignmentEvents(updatedChat, operator, previousStatus, previousOperator, chatMapper.toDto(updatedChat));
                    return null;
                },
                "operation", OPERATION_LINK_OPERATOR,
                KEY_CHAT_ID, chatId,
                KEY_OPERATOR_ID, operatorId,
                KEY_USER_ID, userContext.getUserId()
        );
    }

//    @EventListener
//    @Transactional
//    public void handleAutoResponderEscalation(AutoResponderEscalationEvent event) {
//        log.info("handleAutoResponderEscalation run");
//        MdcUtil.withContext(
//                () -> {
//                    log.info("Received AutoResponderEscalationEvent for chat ID: {}, client ID: {}", event.getChatId(), event.getClientId());
//                    try {
//                        escalateChatToOperatorInternal(event.getChatId(), event.getClientId());
//                        log.info("Handled auto-responder escalation event successfully for chat ID: {}", event.getChatId());
//                    } catch (ChatNotFoundException | ChatServiceException e) {
//                        log.error("Error handling auto-responder escalation for chat ID {}: {}", event.getChatId(), e.getMessage(), e);
//                        metricHelper.incrementChatOperationError(OPERATION_HANDLE_AUTO_ESCALATION,
//                                getCompanyIdFromChatOrUnknown(event.getChatId()), e.getClass().getSimpleName());
//                    } catch (Exception e) {
//                        log.error("Unexpected error handling auto-responder escalation for chat ID {}: {}", event.getChatId(), e.getMessage(), e);
//                        metricHelper.incrementChatOperationError(OPERATION_HANDLE_AUTO_ESCALATION,
//                                getCompanyIdFromChatOrUnknown(event.getChatId()), "UnexpectedException");
//                    }
//                    return null;
//                },
//                "operation", OPERATION_HANDLE_AUTO_ESCALATION,
//                KEY_CHAT_ID, event.getChatId(),
//                KEY_CLIENT_ID, event.getClientId()
//        );
//    }

    private void escalateChatToOperatorInternal(Integer chatId, Integer clientId) {
        log.info("[{}] AutoResponder requesting operator escalation for chat ID {}", OPERATION_ESCALATE_CHAT_INTERNAL, chatId);

        Chat chat = findChatOrThrow(chatId);

        chatValidationUtil.ensureClientOwnsChat(chat, clientId, OPERATION_ESCALATE_CHAT_INTERNAL);
        chatValidationUtil.ensureChatNotClosedOrArchived(chat, "Cannot escalate chat: already closed or archived.");

        MDC.put(KEY_COMPANY_ID_MDC, metricHelper.getCompanyIdStr(chat.getCompany()));
        MDC.put(KEY_CHAT_ID_MDC, String.valueOf(chat.getId()));

        User previouslyAssignedOperator = chat.getUser();
        ChatStatus previousStatus = chat.getStatus();

        Optional<User> operatorOpt = assignmentStrategyService.findOperatorForChat(chat);
        Chat updatedChat;

        if (operatorOpt.isPresent()) {
            User newOperator = operatorOpt.get();
            log.info("[{}] Assigning escalated chat {} to operator {}",OPERATION_ESCALATE_CHAT_INTERNAL, chatId, newOperator.getId());
            updatedChat = updateChatAssignment(chat, newOperator, ChatStatus.ASSIGNED, LocalDateTime.now());
            notificationService.createNotification(newOperator, updatedChat,
                    "CHAT_ASSIGNED_ESCALATION_AUTO", "Вам назначен чат #" + updatedChat.getId() + " после автоматической эскалации.");
        } else {
            log.warn("[{}] No operator found for auto-escalated chat {}. Setting status to PENDING_OPERATOR.", OPERATION_ESCALATE_CHAT_INTERNAL, chatId);
            updatedChat = updateChatStatus(chat, ChatStatus.PENDING_OPERATOR);
        }

        finalizeAndPublishEscalationEvents(updatedChat, previousStatus, previouslyAssignedOperator, operatorOpt.orElse(null));

        metricHelper.recordChatAssignmentTimeIfApplicable(updatedChat);
    }

    private Chat findChatOrThrow(Integer chatId) {
        return chatRepository.findById(chatId)
                .orElseThrow(() -> new ChatNotFoundException("Chat with ID " + chatId + " not found."));
    }

    private User findUserOrThrow(Integer userId) {
        return userService.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User (operator) with ID " + userId + " not found."));
    }

    private String getCompanyIdFromChatOrUnknown(Integer chatId) {
        try {
            return chatRepository.findById(chatId)
                    .map(Chat::getCompany)
                    .map(metricHelper::getCompanyIdStr)
                    .orElse("unknown_chat_or_company");
        } catch (Exception e) {
            log.warn("Failed to get company ID for chat {} during error handling: {}", chatId, e.getMessage());
            return "unknown_error_getting_company";
        }
    }

    private User determineOperatorForAssignment(Chat chat, Integer requestedOperatorId, Integer assignerCompanyId) throws AccessDeniedException {
        if (requestedOperatorId != null) {
            User specificOperator = findUserOrThrow(requestedOperatorId);
            chatValidationUtil.ensureUserBelongsToCompany(specificOperator, assignerCompanyId,
                    "Cannot assign operator from a different company.");

            if (!LeastBusyAssignmentService.ACCEPTING_CHAT_USER_STATUSES.contains(specificOperator.getStatus())) {
                log.warn("Attempt to assign chat {} to operator {} who is not in an accepting status (current: {}).",
                        chat.getId(), specificOperator.getId(), specificOperator.getStatus());
                throw new ChatServiceException("Operator " + specificOperator.getFullName() +
                        " is currently " + specificOperator.getStatus() +
                        " and cannot be assigned new chats at this moment.");
            }


            List<ChatRepository.OperatorChatCount> counts = chatRepository.countChatsByUserIdInAndStatusIn(
                    List.of(specificOperator.getId()), LeastBusyAssignmentService.OPEN_CHAT_STATUSES);
            long currentChats = counts.isEmpty() ? 0L : counts.get(0).getChatCount();

            int maxChats = specificOperator.getMaxConcurrentChats() != null ? specificOperator.getMaxConcurrentChats() : DEFAULT_MAX_CONCURRENT_CHATS_FOR_EXPLICIT_ASSIGN;

            if (currentChats >= maxChats) {
                log.warn("Attempt to assign chat {} to operator {} who has reached capacity ({} / {}). Explicit assignment override might be possible depending on policy.",
                        chat.getId(), specificOperator.getId(), currentChats, maxChats);
                throw new ChatServiceException("Operator " + specificOperator.getFullName() + " has reached their chat capacity ("+maxChats+").");
            }
            return specificOperator;
        } else {
            return assignmentStrategyService.findOperatorForChat(chat)
                    .orElseThrow(() -> {
                        log.warn("No available operator found by strategy for chat {} in company {}", chat.getId(), chat.getCompany().getId());
                        return new ResourceNotFoundException("No available operator found for assignment (considering status and capacity).");
                    });
        }
    }

    private boolean isAlreadyAssignedToOperator(Chat chat, User operator) {
        return chat.getUser() != null &&
                Objects.equals(chat.getUser().getId(), operator.getId()) &&
                (chat.getStatus() == ChatStatus.ASSIGNED || chat.getStatus() == ChatStatus.IN_PROGRESS);
    }

    private ChatDetailsDTO performAssignment(Chat chat, User operator, String operationContext) {
        ChatStatus previousStatus = chat.getStatus();
        User previousOperator = chat.getUser();

        Chat updatedChat = updateChatAssignment(chat, operator, ChatStatus.ASSIGNED, LocalDateTime.now());
        notificationService.createNotification(operator, updatedChat, "CHAT_ASSIGNED", "Вам назначен чат #" + updatedChat.getId());

        metricHelper.recordChatAssignmentTimeIfApplicable(updatedChat);
        metricHelper.incrementChatOperatorLinked(updatedChat.getCompany(), updatedChat.getChatChannel());

        ChatDTO chatDto = chatMapper.toDto(updatedChat);
        publishAssignmentEvents(updatedChat, operator, previousStatus, previousOperator, chatDto);

        log.info("[{}] Chat {} successfully assigned to operator {}", operationContext, updatedChat.getId(), operator.getId());
        return chatMapper.toDetailsDto(findChatOrThrow(updatedChat.getId()));
    }

    private Chat updateChatAssignment(Chat chat, User operator, ChatStatus newStatus, LocalDateTime assignedAt) {
        log.info("UPDATE_CHAT_ASSIGN: Attempting to set chat {} to operator {} and status {}", chat.getId(), operator.getId(), newStatus);
        chat.setUser(operator);
        chat.setStatus(newStatus);
        chat.setAssignedAt(assignedAt);
        return persistChat(chat, "updateChatAssignment", chat.getCompany());
    }

    private Chat updateChatStatus(Chat chat, ChatStatus newStatus) {
        if (chat.getStatus() == newStatus) return chat;
        chat.setStatus(newStatus);
        if (newStatus == ChatStatus.PENDING_OPERATOR) {
            chat.setUser(null);
            chat.setAssignedAt(null);
        }
        return persistChat(chat, "updateChatStatus", chat.getCompany());
    }

    private Chat persistChat(Chat chat, String operationName, Company companyContext) {
        try {
            Chat savedChat = chatRepository.save(chat);
            MDC.put("chatId", String.valueOf(savedChat.getId()));
            log.debug("[{}] Chat entity with ID {} saved/updated.", operationName, savedChat.getId());
            return savedChat;
        } catch (Exception e) {
            metricHelper.incrementChatOperationError(operationName, companyContext, "ChatPersistenceError");
            log.error("[{}] Error persisting chat for company {}: {}", operationName, metricHelper.getCompanyIdStr(companyContext), e.getMessage(), e);
            throw new ChatServiceException("Failed to save chat information during assignment.", e);
        }
    }

    private void publishAssignmentEvents(Chat chat, User newOperator, ChatStatus oldStatus, User oldOperator, ChatDTO chatDto) {
        boolean operatorChanged = oldOperator == null || !Objects.equals(oldOperator.getId(), newOperator.getId());
        boolean statusChanged = oldStatus != chat.getStatus();

        if (operatorChanged) {
            eventPublisher.publishEvent(new ChatAssignedToOperatorEvent(this, chatDto, newOperator.getEmail(), chat.getCompany().getId()));
            log.debug("Published ChatAssignedToOperatorEvent for chat ID {} to operator {}", chat.getId(), newOperator.getEmail());
        }
        if (statusChanged || operatorChanged) {
            eventPublisher.publishEvent(new ChatStatusChangedEvent(this, chat.getId(), chatDto));
            log.debug("Published ChatStatusChangedEvent for chat ID {} (status/operator change)", chat.getId());
        }
    }

    private ChatDetailsDTO finalizeAndPublishEscalationEvents(Chat updatedChat, ChatStatus previousStatus, User previouslyAssignedOperator, User newOperator) {
        ChatDTO chatDto = chatMapper.toDto(updatedChat);
        eventPublisher.publishEvent(new ChatEscalatedToOperatorEvent(this, chatDto, updatedChat.getCompany().getId()));
        log.debug("Published ChatEscalatedToOperatorEvent for chat ID {}", updatedChat.getId());

        boolean operatorAssignedThisTime = newOperator != null;
        boolean operatorChanged = operatorAssignedThisTime && (previouslyAssignedOperator == null || !Objects.equals(previouslyAssignedOperator.getId(), newOperator.getId()));
        boolean statusChanged = previousStatus != updatedChat.getStatus();

        if (operatorAssignedThisTime && operatorChanged) {
            eventPublisher.publishEvent(new ChatAssignedToOperatorEvent(this, chatDto, newOperator.getEmail(), updatedChat.getCompany().getId()));
            log.debug("Published ChatAssignedToOperatorEvent for escalated chat ID {}", updatedChat.getId());
        }

        if (statusChanged) {
            eventPublisher.publishEvent(new ChatStatusChangedEvent(this, updatedChat.getId(), chatDto));
            log.debug("Published ChatStatusChangedEvent for escalated chat ID {}", updatedChat.getId());
            if (updatedChat.getStatus() == ChatStatus.PENDING_OPERATOR && updatedChat.getUser() == null) {
                eventPublisher.publishEvent(new NewPendingChatEvent(this, chatDto, updatedChat.getCompany().getId()));
                log.debug("Published NewPendingChatEvent for escalated chat ID {} (no operator found)", updatedChat.getId());
            }
        }
        return chatMapper.toDetailsDto(findChatOrThrow(updatedChat.getId()));
    }
}
