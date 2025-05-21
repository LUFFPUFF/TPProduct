package com.example.domain.api.chat_service_api.service.impl;

import com.example.database.model.chats_messages_module.chat.Chat;
import com.example.database.model.chats_messages_module.chat.ChatChannel;
import com.example.database.model.chats_messages_module.chat.ChatMessageSenderType;
import com.example.database.model.chats_messages_module.chat.ChatStatus;
import com.example.database.model.chats_messages_module.message.ChatMessage;
import com.example.database.model.chats_messages_module.message.MessageStatus;
import com.example.database.model.company_subscription_module.company.Company;
import com.example.database.model.company_subscription_module.user_roles.user.Role;
import com.example.database.model.company_subscription_module.user_roles.user.User;
import com.example.database.model.crm_module.client.Client;
import com.example.database.repository.chats_messages_module.ChatMessageRepository;
import com.example.database.repository.chats_messages_module.ChatRepository;
import com.example.database.repository.company_subscription_module.UserRepository;
import com.example.domain.api.ans_api_module.event.AutoResponderEscalationEvent;
import com.example.domain.api.ans_api_module.exception.AutoResponderException;
import com.example.domain.api.ans_api_module.service.IAutoResponderService;
import com.example.domain.api.chat_service_api.exception_handler.ChatNotFoundException;
import com.example.domain.api.chat_service_api.exception_handler.ResourceNotFoundException;
import com.example.domain.api.chat_service_api.exception_handler.exception.service.ChatServiceException;
import com.example.domain.api.chat_service_api.mapper.ChatMapper;
import com.example.domain.api.chat_service_api.model.dto.ChatDTO;
import com.example.domain.api.chat_service_api.model.dto.ChatDetailsDTO;
import com.example.domain.api.chat_service_api.model.dto.MessageDto;
import com.example.domain.api.chat_service_api.model.rest.chat.AssignChatRequestDTO;
import com.example.domain.api.chat_service_api.model.rest.chat.CloseChatRequestDTO;
import com.example.domain.api.chat_service_api.model.rest.chat.CreateChatRequestDTO;
import com.example.domain.api.chat_service_api.model.rest.mesage.SendMessageRequestDTO;
import com.example.domain.api.chat_service_api.service.*;

import com.example.domain.api.statistics_module.aop.annotation.Counter;
import com.example.domain.api.statistics_module.aop.annotation.MeteredOperation;
import com.example.domain.api.statistics_module.aop.annotation.Tag;
import com.example.domain.api.statistics_module.metrics.service.IChatMetricsService;
import com.example.domain.security.model.UserContext;
import com.example.domain.security.util.UserContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.event.EventListener;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.file.AccessDeniedException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.example.domain.api.chat_service_api.service.impl.LeastBusyAssignmentService.OPEN_CHAT_STATUSES;

@Service
@RequiredArgsConstructor
@Slf4j
@MeteredOperation(prefix = "chat_app_")
//TODO полный рефакторинг ChatServiceImpl
public class ChatServiceImpl implements IChatService {

    private final ChatRepository chatRepository;
    private final IClientService clientService;
    private final IUserService userService;
    private final IChatMessageService chatMessageService;
    private final ChatMapper chatMapper;
    private final ChatMessageRepository chatMessageRepository;
    private final IAssignmentService assignmentService;
    private final WebSocketMessagingService messagingService;
    private final INotificationService notificationService;
    private final IAutoResponderService autoResponderService;
    private final IChatMetricsService chatMetricsService;

    @Override
    @Transactional
    @MeteredOperation(
            counters = {
                    @Counter(
                            name = "chats_created_total",
                            tags = {
                                    @Tag(key = "company_id", valueSpEL = "#result != null && #result.companyId != null ? T(com.example.domain.api.statistics_module.metrics.util.MetricsTagSanitizer).sanitize(#result.companyId.toString()) : 'unknown'"),
                                    @Tag(key = "channel", valueSpEL = "#result != null && #result.chatChannel != null ? T(com.example.domain.api.statistics_module.metrics.util.MetricsTagSanitizer).sanitize(#result.chatChannel.toString()) : 'UNKNOWN'"),
                                    @Tag(key = "from_operator_ui", valueSpEL = "'false'")
                            }
                    ),
                    @Counter(
                            name = "chats_auto_responder_handled_total",
                            conditionSpEL = "#result.status?.name() == 'PENDING_AUTO_RESPONDER'",
                            tags = {
                                    @Tag(key = "company_id", valueSpEL = "#result.companyId ?: 'unknown'"),
                                    @Tag(key = "channel", valueSpEL = "#result.chatChannel?.name() ?: 'UNKNOWN'")
                            }
                    )
            }
    )
    public ChatDetailsDTO createChat(CreateChatRequestDTO createRequest) {
        Client client = clientService.findById(createRequest.getClientId())
                .orElseThrow(() -> new ResourceNotFoundException("Client with ID " + createRequest.getClientId() + " not found"));

        Company company = client.getCompany();
        String companyIdStrForErrorHandling = "unknown";
        if (company == null) {
            log.error("Client with ID {} is not associated with a company.", createRequest.getClientId());
            chatMetricsService.incrementChatOperationError("createChat", companyIdStrForErrorHandling, "ClientNotAssociatedWithCompany");
            throw new ResourceNotFoundException("Client is not associated with a company.");
        }
        companyIdStrForErrorHandling = company.getId() != null ? company.getId().toString() : "unknown";


        Optional<Chat> existingChat = findOpenChatByClientAndChannel(createRequest.getClientId(), createRequest.getChatChannel());
        if (existingChat.isPresent()) {
            log.warn("Open chat ID {} already exists for client {} on channel {}", existingChat.get().getId(), createRequest.getClientId(), createRequest.getChatChannel());
            chatMetricsService.incrementChatOperationError("createChat", companyIdStrForErrorHandling, "OpenChatAlreadyExists");
            throw new ChatServiceException("Open chat already exists for this client and channel.");
        }

        Chat chat = createChatEntity(client, company, createRequest);
        log.debug("Created initial chat entity with ID: {}", chat.getId());

        try {
            Chat savedChat = chatRepository.save(chat);
            log.info("Saved initial chat entity with ID: {}", savedChat.getId());

            if (createRequest.getInitialMessageContent() != null && !createRequest.getInitialMessageContent().isBlank()) {
                ChatMessage firstMessage = createChatMessageEntity(savedChat, client, null,
                        ChatMessageSenderType.CLIENT, createRequest.getInitialMessageContent(), null, null);
                chatMessageRepository.save(firstMessage);
                savedChat.setLastMessageAt(firstMessage.getSentAt());
            } else {
                savedChat.setLastMessageAt(savedChat.getCreatedAt());
            }

            chatRepository.save(savedChat);

            try {
                autoResponderService.processNewPendingChat(savedChat.getId());
                log.info("Auto-responder triggered for chat ID: {}", savedChat.getId());
            } catch (AutoResponderException e) {
                log.error("Failed to trigger auto-responder for chat ID {}: {}", savedChat.getId(), e.getMessage(), e);
                chatMetricsService.incrementChatOperationError("createChat_AutoResponder", companyIdStrForErrorHandling, e.getClass().getSimpleName());
                throw e;
            }

            return chatMapper.toDetailsDto(savedChat);
        } catch (ResourceNotFoundException | ChatServiceException | AutoResponderException e) {
            if (!(e instanceof AutoResponderException)) {
                chatMetricsService.incrementChatOperationError("createChat", companyIdStrForErrorHandling, e.getClass().getSimpleName());
            }
            throw e;
        } catch (Exception e) {
            chatMetricsService.incrementChatOperationError("createChat", companyIdStrForErrorHandling, "UnexpectedException");
            log.error("Unexpected error in createChat for client {}: {}", createRequest.getClientId(), e.getMessage(), e);
            throw new ChatServiceException("An unexpected error occurred while creating the chat.", e);
        }
    }

    @Override
    @Transactional
    @MeteredOperation(
            counters = {
                    @Counter(name = "chats_created_total",
                            tags = { @Tag(key = "company_id", valueSpEL = "#result.companyId ?: 'unknown'"),
                                    @Tag(key = "channel", valueSpEL = "#result.chatChannel?.name() ?: 'UNKNOWN'"),
                                    @Tag(key = "from_operator_ui", valueSpEL = "'true'") }),
                    @Counter(name = "chats_assigned_total",
                            tags = { @Tag(key = "company_id", valueSpEL = "#result.companyId ?: 'unknown'"),
                                    @Tag(key = "channel", valueSpEL = "#result.chatChannel?.name() ?: 'UNKNOWN'"),
                                    @Tag(key = "auto_assigned", valueSpEL = "'false'") }),
                    @Counter(name = "chats_operator_linked_total",
                            tags = { @Tag(key = "company_id", valueSpEL = "#result.companyId ?: 'unknown'"),
                                    @Tag(key = "channel", valueSpEL = "#result.chatChannel?.name() ?: 'UNKNOWN'") })
            }
    )
    public ChatDetailsDTO createChatWithOperatorFromUI(CreateChatRequestDTO createRequest) throws AccessDeniedException {
        UserContext userContext = UserContextHolder.getRequiredContext();
        User currentUser = userService.findById(userContext.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found."));

        Client client = getClientWithCompanyCheck(createRequest.getClientId());
        String companyIdStrForErrorHandling = (currentUser.getCompany() != null && currentUser.getCompany().getId() != null) ? currentUser.getCompany().getId().toString() : "unknown";

        if (!Objects.equals(Objects.requireNonNull(currentUser.getCompany()).getId(), client.getCompany().getId())) {
            chatMetricsService.incrementChatOperationError("createChatWithOperatorFromUI", companyIdStrForErrorHandling, "CompanyMismatch");
            log.warn("User ID {} from company {} attempted to create chat with client ID {} from different company {}",
                    currentUser.getId(), currentUser.getCompany().getId(), client.getId(), client.getCompany().getId());
            throw new AccessDeniedException("Access Denied: Cannot create chat with a client from a different company.");
        }

        Optional<Chat> existingChat = findOpenChatByClientAndChannel(createRequest.getClientId(), createRequest.getChatChannel());
        if (existingChat.isPresent()) {
            Chat foundChat = existingChat.get();
            if (!Objects.equals(currentUser.getCompany().getId(), foundChat.getCompany().getId())) {
                chatMetricsService.incrementChatOperationError("createChatWithOperatorFromUI", companyIdStrForErrorHandling, "ExistingChatDifferentCompany");
                throw new AccessDeniedException("Access Denied: Existing chat belongs to a different company.");
            }
            return chatMapper.toDetailsDto(foundChat);
        }

        Chat chat = createChatEntityWithOperator(client, currentUser, createRequest.getChatChannel());

        try {
            Chat savedChat = chatRepository.save(chat);
            addInitialMessageIfNeeded(savedChat, client, createRequest.getInitialMessageContent());

            if (savedChat.getAssignedAt() != null && savedChat.getCreatedAt() != null) {
                chatMetricsService.recordChatAssignmentTime(
                        companyIdStrForErrorHandling,
                        savedChat.getChatChannel() != null ? savedChat.getChatChannel() : ChatChannel.UNKNOWN,
                        Duration.between(savedChat.getCreatedAt(), savedChat.getAssignedAt())
                );
            }
            return chatMapper.toDetailsDto(savedChat);
        } catch (Exception e) {
            chatMetricsService.incrementChatOperationError("createChatWithOperatorFromUI_Save", companyIdStrForErrorHandling, e.getClass().getSimpleName());
            log.error("Unexpected error in createChatWithOperatorFromUI during save for client {}: {}", createRequest.getClientId(), e.getMessage(), e);
            throw new ChatServiceException("An unexpected error occurred.", e);
        }
    }

    private Chat createChatEntityWithOperator(Client client, User operator, ChatChannel channel) {
        Chat chat = new Chat();
        chat.setClient(client);
        chat.setCompany(client.getCompany());
        chat.setChatChannel(channel);
        chat.setStatus(ChatStatus.ASSIGNED);
        chat.setUser(operator);

        chat.setCreatedAt(LocalDateTime.now());
        chat.setAssignedAt(LocalDateTime.now());
        chat.setLastMessageAt(LocalDateTime.now());

        return chat;
    }

    private void addInitialMessageIfNeeded(Chat chat, Client client, String messageContent) {
        if (StringUtils.hasText(messageContent)) {
            ChatMessage firstMessage = new ChatMessage();
            firstMessage.setChat(chat);
            firstMessage.setSenderClient(client);
            firstMessage.setSenderType(ChatMessageSenderType.CLIENT);
            firstMessage.setContent(messageContent);
            firstMessage.setSentAt(LocalDateTime.now());

            chatMessageRepository.save(firstMessage);
            chat.setLastMessageAt(firstMessage.getSentAt());
            chatRepository.save(chat);
        }
    }

    private Client getClientWithCompanyCheck(Integer clientId) {
        Client client = clientService.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client with ID " + clientId + " not found"));

        if (client.getCompany() == null) {
            log.error("Client with ID {} is not associated with a company.", clientId);
            throw new ResourceNotFoundException("Client is not associated with a company");
        }

        return client;
    }

    @Override
    @Transactional
    @MeteredOperation(
            counters = @Counter(name = "chats_escalated_total",
                    tags = { @Tag(key = "company_id", valueSpEL = "#result.companyId ?: 'unknown'"),
                            @Tag(key = "channel", valueSpEL = "#result.chatChannel?.name() ?: 'UNKNOWN'")})
    )
    public ChatDetailsDTO requestOperatorEscalation(Integer chatId, Integer clientId) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new ChatNotFoundException("Chat with ID " + chatId + " not found"));

        String companyIdStr = (chat.getCompany() != null && chat.getCompany().getId() != null) ? chat.getCompany().getId().toString() : "unknown";
        ChatChannel channel = chat.getChatChannel() != null ? chat.getChatChannel() : ChatChannel.UNKNOWN;

        if (!chat.getClient().getId().equals(clientId)) {
            chatMetricsService.incrementChatOperationError("requestOperatorEscalation", companyIdStr, "ClientMismatch");
            throw new ChatServiceException("You are not the client of this chat.");
        }
        if (chat.getStatus() == ChatStatus.CLOSED || chat.getStatus() == ChatStatus.ARCHIVED) {
            chatMetricsService.incrementChatOperationError("requestOperatorEscalation", companyIdStr, "ChatAlreadyClosed");
            throw new ChatServiceException("Chat ID " + chatId + " is already closed.");
        }

        autoResponderService.stopForChat(chatId);

        User operatorToAssign = null;
        if (chat.getUser() == null) {
            Optional<User> operatorOpt = assignmentService.findLeastBusyOperator(chat.getCompany().getId());
            if (operatorOpt.isPresent()) {
                operatorToAssign = operatorOpt.get();
                chat.setUser(operatorToAssign);
                notificationService.createNotification(operatorToAssign, chat,
                        "CHAT_ASSIGNED", "Вам назначен чат #" + chat.getId());
            } else {
                messagingService.sendMessage("/topic/operators/available/new-chats", chatMapper.toDto(chat));
            }
        }

        try {
            chat.setStatus(ChatStatus.ASSIGNED);
            if (chat.getAssignedAt() == null && operatorToAssign != null) {
                chat.setAssignedAt(LocalDateTime.now());
            }
            Chat updatedChat = chatRepository.save(chat);

            if (updatedChat.getAssignedAt() != null && updatedChat.getCreatedAt() != null && operatorToAssign != null) {
                chatMetricsService.recordChatAssignmentTime(
                        companyIdStr,
                        channel,
                        Duration.between(updatedChat.getCreatedAt(), updatedChat.getAssignedAt())
                );
            }

            messagingService.sendMessage("/topic/chat/" + chat.getId() + "/status", chatMapper.toDto(updatedChat));
            return chatMapper.toDetailsDto(updatedChat);
        } catch (Exception e) {
            chatMetricsService.incrementChatOperationError("requestOperatorEscalation_Save", companyIdStr, e.getClass().getSimpleName());
            throw new ChatServiceException("An unexpected error occurred during escalation save.", e);
        }
    }

    @EventListener
    public void handleAutoResponderEscalationEvent(AutoResponderEscalationEvent event) {
        log.info("Received AutoResponderEscalationEvent for chat ID: {}", event.getChatId());
        try {
            requestOperatorEscalation(event.getChatId(), event.getClientId());
            log.info("Handled escalation event successfully for chat ID: {}", event.getChatId());
        } catch (Exception e) {
            log.error("Error handling escalation event for chat ID {}: {}", event.getChatId(), e.getMessage(), e);
            throw new ChatServiceException(e);
        }
    }

    @Override
    @Transactional
    @MeteredOperation(
            counters = @Counter(name = "chats_operator_linked_total",
                    tags = {@Tag(key = "company_id", valueSpEL = "#chat.company?.id?.toString() ?: 'unknown'"),
                            @Tag(key = "channel", valueSpEL = "#chat.chatChannel?.name() ?: 'UNKNOWN'")})
    )
    public void linkOperatorToChat(Integer chatId, Integer operatorId) {
        UserContext userContext = UserContextHolder.getRequiredContext();
        User currentUser = userService.findById(userContext.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found."));

        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new ChatNotFoundException("Chat with ID " + chatId + " not found for linking operator."));

        String companyIdStr = (chat.getCompany() != null && chat.getCompany().getId() != null) ? chat.getCompany().getId().toString() : "unknown";
        ChatChannel channel = chat.getChatChannel() != null ? chat.getChatChannel() : ChatChannel.UNKNOWN;

        User operator = userService.findById(operatorId)
                .orElseThrow(() -> new ResourceNotFoundException("Operator with ID " + operatorId + " not found for linking."));

        if (chat.getCompany() == null || operator.getCompany() == null || !chat.getCompany().getId().equals(operator.getCompany().getId()) ||
                !chat.getCompany().getId().equals(currentUser.getCompany().getId())) {
            throw new ChatServiceException("Operator, chat, and current user must belong to the same company to link.");
        }

        try {
            chat.setUser(operator);
            chatRepository.save(chat);
        } catch (Exception e) {
            chatMetricsService.incrementChatOperationError("linkOperatorToChat", companyIdStr, e.getClass().getSimpleName());
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Chat> findOpenChatByClientAndChannel(Integer clientId, ChatChannel channel) {
        Optional<Chat> foundChat = chatRepository
                .findFirstByClientIdAndChatChannelAndStatusInOrderByCreatedAtDesc(clientId, channel, OPEN_CHAT_STATUSES);

        if(foundChat.isPresent()) {
            log.debug("Found open chat ID {} for client {} on channel {}", foundChat.get().getId(), clientId, channel);
        } else {
            log.debug("No open chat found for client {} on channel {}", clientId, channel);
        }
        return foundChat;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Chat> findOpenChatByClientAndChannelAndExternalId(Integer clientId, ChatChannel channel, String externalChatId) {
        if (clientId == null || channel == null || externalChatId == null || externalChatId.trim().isEmpty()) {
            log.warn("Attempted to search for chat with null/empty parameters: clientId={}, channel={}, externalChatId={}", clientId, channel, externalChatId);
            return Optional.empty();
        }

        Optional<Chat> foundChat = chatRepository
                .findFirstByClientIdAndChatChannelAndExternalChatIdAndStatusInOrderByCreatedAtDesc(
                        clientId, channel, externalChatId, OPEN_CHAT_STATUSES);

        if(foundChat.isPresent()) {
            log.debug("Found open chat ID {} for client {} on channel {} with external ID {}",
                    foundChat.get().getId(), clientId, channel, externalChatId);
        } else {
            log.debug("No open chat found for client {} on channel {} with external ID {}",
                    clientId, channel, externalChatId);
        }
        return foundChat;
    }


    @Override
    @Transactional(readOnly = true)
    public Optional<Chat> findOpenChatByClient(Integer clientId) {
        return chatRepository.findByClientId(clientId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Chat> findChatEntityById(Integer chatId) {
        log.debug("Finding chat entity by ID: {}", chatId);
        return chatRepository.findById(chatId);
    }

    @Override
    @Transactional
    @MeteredOperation(
            counters = @Counter(name = "chats_assigned_total",
                    tags = { @Tag(key = "company_id", valueSpEL = "#result.companyId ?: 'unknown'"),
                            @Tag(key = "channel", valueSpEL = "#result.chatChannel?.name() ?: 'UNKNOWN'"),
                            @Tag(key = "auto_assigned", valueSpEL = "#assignRequest.operatorId == null ? 'true' : 'false'")})
    )
    public ChatDetailsDTO assignChat(AssignChatRequestDTO assignRequest) throws AccessDeniedException {
        UserContext userContext = UserContextHolder.getRequiredContext();

        Chat chat = chatRepository.findById(assignRequest.getChatId())
                .orElseThrow(() -> new ChatNotFoundException("Chat with ID " + assignRequest.getChatId() + " not found"));

        String companyIdStr = (chat.getCompany() != null && chat.getCompany().getId() != null) ? chat.getCompany().getId().toString() : "unknown";
        ChatChannel channel = chat.getChatChannel() != null ? chat.getChatChannel() : ChatChannel.UNKNOWN;
        LocalDateTime previousAssignedAt = chat.getAssignedAt();

        if (chat.getCompany() == null || !Objects.equals(chat.getCompany().getId(), userContext.getCompanyId())) {
            throw new AccessDeniedException("Access Denied: Chat belongs to a different company.");
        }


        if (chat.getStatus() != ChatStatus.PENDING_OPERATOR) {
            throw new ChatServiceException("Chat with ID " + assignRequest.getChatId() + " is not in a state to be assigned.");
        }

        try {
            User operatorToAssign;
            if (assignRequest.getOperatorId() != null) {
                operatorToAssign = userService.findById(assignRequest.getOperatorId())
                        .orElseThrow(() -> new ResourceNotFoundException("Operator with ID " + assignRequest.getOperatorId() + " not found."));
                if (operatorToAssign.getCompany() == null || !Objects.equals(operatorToAssign.getCompany().getId(), userContext.getCompanyId())) {
                    throw new ChatServiceException("Cannot assign an operator from a different company.");
                }

            } else {
                Optional<User> autoAssignedOperator = assignmentService.assignOperator(chat);
                operatorToAssign = autoAssignedOperator.orElseThrow(() ->
                        new ResourceNotFoundException("No available operator found for assignment for company " + chat.getCompany().getId()));
            }

            if (chat.getStatus() == ChatStatus.ASSIGNED && chat.getUser() != null && Objects.equals(chat.getUser().getId(), operatorToAssign.getId())) {
                return chatMapper.toDetailsDto(chatRepository.findById(chat.getId()).orElseThrow());
            }

            boolean isAutoAssigned = assignRequest.getOperatorId() == null;

            chat.setUser(operatorToAssign);
            chat.setStatus(ChatStatus.ASSIGNED);
            chat.setAssignedAt(LocalDateTime.now());
            if (previousAssignedAt == null) {
                chat.setAssignedAt(LocalDateTime.now());
            }

            Chat updatedChat = chatRepository.save(chat);

            if (updatedChat.getAssignedAt() != null && updatedChat.getCreatedAt() != null) {
                chatMetricsService.recordChatAssignmentTime(
                        companyIdStr,
                        channel,
                        Duration.between(updatedChat.getCreatedAt(), updatedChat.getAssignedAt())
                );
            }
            chatMetricsService.incrementChatOperatorLinked(companyIdStr, channel);

            notificationService.createNotification(operatorToAssign, updatedChat, "CHAT_ASSIGNED", "Вам назначен чат #" + updatedChat.getId());
            if (chat.getStatus() == ChatStatus.PENDING_OPERATOR) {
                messagingService.sendMessage("/topic/operators/available/chat-assignment", chatMapper.toDto(updatedChat));
            }

            return chatMapper.toDetailsDto(chatRepository.findById(updatedChat.getId()).orElseThrow());
        } catch (ChatNotFoundException | ResourceNotFoundException | ChatServiceException e) {
            chatMetricsService.incrementChatOperationError("assignChat", companyIdStr, e.getClass().getSimpleName());
            throw e;
        } catch (Exception e) {
            chatMetricsService.incrementChatOperationError("assignChat", companyIdStr, "UnexpectedException");
            throw new ChatServiceException("An unexpected error occurred.", e);
        }
    }

    @Override
    @Transactional
    @MeteredOperation(
            counters = @Counter(name = "chats_closed_total",
                    tags = { @Tag(key = "company_id", valueSpEL = "#result.companyId ?: 'unknown'"),
                            @Tag(key = "channel", valueSpEL = "#result.chatChannel?.name() ?: 'UNKNOWN'"),
                            @Tag(key = "final_status", valueSpEL = "'CLOSED'")})
    )
    public ChatDetailsDTO closeChatByCurrentUser(Integer chatId) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new ChatNotFoundException("Chat with ID " + chatId + " not found"));

        String companyIdStr = (chat.getCompany() != null && chat.getCompany().getId() != null) ? chat.getCompany().getId().toString() : "unknown";
        ChatChannel channel = chat.getChatChannel() != null ? chat.getChatChannel() : ChatChannel.UNKNOWN;

        if (chat.getStatus() == ChatStatus.CLOSED || chat.getStatus() == ChatStatus.ARCHIVED) {
            throw new ChatServiceException("Chat with ID " + chatId + " is already closed or archived.");
        }

        try {
            chat.setStatus(ChatStatus.CLOSED);
            chat.setClosedAt(LocalDateTime.now());

            Chat closedChat = chatRepository.save(chat);

            if (closedChat.getCreatedAt() != null && closedChat.getClosedAt() != null) {
                chatMetricsService.recordChatDuration(
                        companyIdStr,
                        channel,
                        ChatStatus.CLOSED,
                        Duration.between(closedChat.getCreatedAt(), closedChat.getClosedAt())
                );
            }

            if (closedChat.getUser() != null) {
                notificationService.createNotification(closedChat.getUser(), closedChat, "CHAT_CLOSED", "Чат #" + closedChat.getId() + " был закрыт.");
            }

            messagingService.sendMessage("/topic/chat/" + closedChat.getId() + "/status", chatMapper.toDto(closedChat));

            return chatMapper.toDetailsDto(chatRepository.findById(closedChat.getId()).orElseThrow());
        } catch (ChatNotFoundException | ChatServiceException e) {
            chatMetricsService.incrementChatOperationError("closeChatByCurrentUser", companyIdStr, e.getClass().getSimpleName());
            throw e;
        } catch (Exception e) {
            chatMetricsService.incrementChatOperationError("closeChatByCurrentUser", companyIdStr, "UnexpectedException");
            throw new ChatServiceException("An unexpected error occurred.", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ChatDetailsDTO getChatDetails(Integer chatId) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new ChatNotFoundException("Chat with ID " + chatId + " not found"));

        return chatMapper.toDetailsDto(chat);
    }

    @Override
    @Transactional(readOnly = true)
    @Deprecated
    public List<ChatDTO> getOperatorChats(Integer userId) {
        Collection<ChatStatus> assignedStatuses = Set.of(ChatStatus.ASSIGNED, ChatStatus.IN_PROGRESS);
        List<Chat> chats = chatRepository.findByUserIdAndStatusIn(userId, assignedStatuses);

        return chats.stream()
                .map(chat -> {
                   ChatDTO chatDTO = chatMapper.toDto(chat);
                   chatDTO.setLastMessageSnippet(chat.getMessages().get(chat.getMessages().size()-1).getContent());
                   return chatDTO;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    @Deprecated
    public List<ChatDTO> getOperatorChatsStatus(Integer userId, Set<ChatStatus> statuses) {
        List<Chat> chats = chatRepository.findByUserIdAndStatusIn(userId, statuses);
        return chats.stream()
                .map(chatMapper::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)

    public List<ChatDTO> getMyChats(Set<ChatStatus> statuses) throws AccessDeniedException {
        UserContext userContext = UserContextHolder.getRequiredContext();
        Integer currentUserId = userContext.getUserId();
        Integer userCompanyId = userContext.getCompanyId();
        Set<Role> userRoles = userContext.getRoles();

        List<Chat> chats;
        if (userRoles.contains(Role.MANAGER)) {
            if (statuses == null || statuses.isEmpty()) {
                Collection<ChatStatus> defaultManagerStatuses = Set.of(ChatStatus.ASSIGNED, ChatStatus.IN_PROGRESS, ChatStatus.PENDING_OPERATOR);
                chats = chatRepository.findByCompanyIdAndStatusIn(userCompanyId, defaultManagerStatuses);
            } else {
                chats = chatRepository.findByCompanyIdAndStatusIn(userCompanyId, statuses);
            }
        } else if (userRoles.contains(Role.OPERATOR)) {
            if (statuses == null || statuses.isEmpty()) {
                Collection<ChatStatus> defaultOperatorStatuses = Set.of(ChatStatus.ASSIGNED, ChatStatus.IN_PROGRESS);
                chats = chatRepository.findByUserIdAndStatusIn(currentUserId, defaultOperatorStatuses);
            } else {
                chats = chatRepository.findByUserIdAndStatusIn(currentUserId, statuses);
            }
        } else {
            throw new AccessDeniedException("Access Denied: Only Managers and Operators can list chats.");
        }

        return getChatDTOS(chats);
    }

    @Override
    @Transactional(readOnly = true)

    public List<ChatDTO> getChatsForCurrentUser(Set<ChatStatus> statuses) {
        UserContext userContext = UserContextHolder.getRequiredContext();
        Integer currentUserId = userContext.getUserId();
        Integer userCompanyId = userContext.getCompanyId();
        Set<Role> userRoles = userContext.getRoles();

        log.debug("User ID {} (Roles: {}, Company: {}) requesting their assigned chats with statuses: {}",
                currentUserId, userRoles, userCompanyId, statuses);

        Collection<ChatStatus> effectiveStatuses = (statuses == null || statuses.isEmpty())
                ? Set.of(ChatStatus.ASSIGNED, ChatStatus.IN_PROGRESS)
                : statuses;

        List<Chat> chats = chatRepository.findByUserIdAndStatusIn(currentUserId, effectiveStatuses);

        return getChatDTOS(chats);
    }

    @NotNull
    private List<ChatDTO> getChatDTOS(List<Chat> chats) {
        return chats.stream()
                .map(chat -> {
                    ChatDTO chatDTO = chatMapper.toDto(chat);
                    if (chat.getMessages() != null && !chat.getMessages().isEmpty()) {
                        try {
                            chatDTO.setLastMessageSnippet(chat.getMessages().get(chat.getMessages().size()-1).getContent());
                        } catch (Exception e) {
                            chatDTO.setLastMessageSnippet("");
                        }
                    } else {
                        chatDTO.setLastMessageSnippet("");
                    }
                    return chatDTO;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)

    public List<ChatDTO> getOperatorChats(Integer operatorId, Set<ChatStatus> statuses) throws AccessDeniedException {
        UserContext userContext = UserContextHolder.getRequiredContext();
        Integer userCompanyId = userContext.getCompanyId();

        User targetOperator = userService.findById(operatorId)
                .orElseThrow(() -> new ResourceNotFoundException("Operator with ID " + operatorId + " not found."));

        if (targetOperator.getCompany() == null || !Objects.equals(userCompanyId, targetOperator.getCompany().getId())) {
            throw new AccessDeniedException("Access Denied: Cannot view chats for an operator in a different company.");
        }

        Collection<ChatStatus> effectiveStatuses = (statuses == null || statuses.isEmpty())
                ? Set.of(ChatStatus.ASSIGNED, ChatStatus.IN_PROGRESS)
                : statuses;

        List<Chat> chats = chatRepository.findByUserIdAndStatusIn(operatorId, effectiveStatuses);

        return chats.stream()
                .map(chatMapper::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatDTO> getClientChats(Integer clientId) {
        // TODO: Реализовать получение чатов для клиента.
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    @Transactional(readOnly = true)

    public List<ChatDTO> getMyCompanyWaitingChats() throws AccessDeniedException {
        UserContext userContext = UserContextHolder.getRequiredContext();
        Integer userCompanyId = userContext.getCompanyId();

        if (userCompanyId == null) {
            throw new AccessDeniedException("Access Denied: User is not associated with a company.");
        }

        List<Chat> chats = chatRepository.findByCompanyIdAndStatusOrderByLastMessageAtDesc(userCompanyId, ChatStatus.PENDING_OPERATOR);

        return chats.stream()
                .map(chatMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional

    public void updateChatStatus(Integer chatId, ChatStatus newStatus) {
        UserContext userContext = UserContextHolder.getRequiredContext();

        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new ChatNotFoundException("Chat with ID " + chatId + " not found"));


        chat.setStatus(newStatus);
        if (newStatus == ChatStatus.CLOSED) {
            chat.setClosedAt(LocalDateTime.now());
        } else if (newStatus == ChatStatus.ASSIGNED) {
            if (chat.getAssignedAt() == null) chat.setAssignedAt(LocalDateTime.now());
        }

        chatRepository.save(chat);
        log.info("Chat ID {} status changed to {} by user {}", chatId, newStatus, userContext.getUserId());

        messagingService.sendMessage("/topic/chat/" + chatId + "/status", chatMapper.toDto(chat));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Chat> findChatByExternalId(Integer companyId, Integer clientId, ChatChannel channel, String externalChatId) {
        throw new UnsupportedOperationException("findChatByExternalId Not implemented yet");
    }

    private Chat createChatEntity(Client client, Company company, CreateChatRequestDTO createChatRequestDTO) {
        Chat chat = new Chat();
        chat.setClient(client);
        chat.setCompany(company);
        chat.setChatChannel(createChatRequestDTO.getChatChannel());
        chat.setStatus(ChatStatus.PENDING_AUTO_RESPONDER);
        chat.setCreatedAt(LocalDateTime.now());
        chat.setUser(null);
        chat.setAssignedAt(null);
        chat.setClosedAt(null);
        chat.setExternalChatId(createChatRequestDTO.getExternalChatId() != null ? createChatRequestDTO.getExternalChatId() : null);
        log.debug("Created initial chat entity.");

        return chat;
    }

    private ChatMessage createChatMessageEntity(Chat chat,
                                                Client senderClient,
                                                User senderOperator,
                                                ChatMessageSenderType senderType,
                                                String content,
                                                String externalMessageId,
                                                String replyToExternalMessageId) {
        ChatMessage message = new ChatMessage();
        message.setChat(chat);
        message.setSenderClient(senderClient);
        message.setSenderOperator(senderOperator);
        message.setSenderType(senderType);
        message.setContent(content);
        message.setExternalMessageId(externalMessageId);
        message.setReplyToExternalMessageId(replyToExternalMessageId);
        message.setSentAt(LocalDateTime.now());
        message.setStatus(MessageStatus.SENT);

        return message;
    }

    @Override
    @Transactional
    @MeteredOperation(
            counters = @Counter(name = "operator_messages_sent_total",
                    tags = { @Tag(key="company_id", valueSpEL="#chatEntity.company?.id?.toString() ?: 'unknown'"),
                            @Tag(key="operator_id", valueSpEL="#userContext.userId?.toString() ?: 'unknown'"),
                            @Tag(key="channel", valueSpEL="#chatEntity.chatChannel?.name() ?: 'UNKNOWN'")})
    )
    public MessageDto sendOperatorMessage(Integer chatId, String content) {
        UserContext userContext = UserContextHolder.getRequiredContext();
        Integer currentUserId = userContext.getUserId();

        Chat chatEntity = chatRepository.findById(chatId)
                .orElseThrow(() -> {
                    log.warn("Chat with ID {} not found for message sending.", chatId);
                    return new ChatNotFoundException("Chat with ID " + chatId + " not found.");
                });

        if (chatEntity.getStatus() == ChatStatus.PENDING_AUTO_RESPONDER ||
                chatEntity.getStatus() == ChatStatus.CLOSED ||
                chatEntity.getStatus() == ChatStatus.ARCHIVED) {
            throw new ChatServiceException("Cannot send message to chat with status: " + chatEntity.getStatus() + ". Allowed statuses: ASSIGNED, IN_PROGRESS.");
        }

        SendMessageRequestDTO serviceRequest = new SendMessageRequestDTO();
        serviceRequest.setChatId(chatEntity.getId());
        serviceRequest.setContent(content);

        serviceRequest.setSenderId(currentUserId);
        serviceRequest.setSenderType(ChatMessageSenderType.OPERATOR);

        return chatMessageService.processAndSaveMessage(
                serviceRequest,
                serviceRequest.getSenderId(),
                serviceRequest.getSenderType()
        );
    }

    @Override
    @Transactional
    public ChatDetailsDTO createTestChatForCurrentUser() throws AccessDeniedException {
        UserContext userContext = UserContextHolder.getRequiredContext();
        Integer currentUserId = userContext.getUserId();
        Integer companyId = userContext.getCompanyId();

        if (companyId == null) {
            throw new AccessDeniedException("Authenticated user is not associated with a company.");
        }

        User operator = userService.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated operator with ID " + currentUserId + " not found."));

        String testClientName = "Тестовый клиент (Оператор " + operator.getFullName() + ")";
        Client testClient = clientService.findByNameAndCompanyId(testClientName, companyId)
                .orElseGet(() -> {
                    return clientService.createClient(testClientName, companyId, null);
                });

        Optional<Chat> existingOpenTestChat = chatRepository.findFirstByClientIdAndCompanyIdAndChatChannelAndStatusInOrderByCreatedAtDesc(
                testClient.getId(), companyId, ChatChannel.Test, OPEN_CHAT_STATUSES);

        if (existingOpenTestChat.isPresent()) {
            return chatMapper.toDetailsDto(existingOpenTestChat.get());
        }

        CreateChatRequestDTO createRequest = new CreateChatRequestDTO();
        createRequest.setClientId(testClient.getId());
        createRequest.setChatChannel(ChatChannel.VK);
        createRequest.setInitialMessageContent("Добрый день! Я тестовый клиент.");

        Chat chat = createChatEntityWithOperator(testClient, operator, ChatChannel.VK);
        Chat savedChat = chatRepository.save(chat);

        addInitialMessageIfNeeded(savedChat, testClient, createRequest.getInitialMessageContent());

        return chatMapper.toDetailsDto(savedChat);
    }

    @Override
    @Transactional
    public void markClientMessagesAsReadByCurrentUser(Integer chatId, Collection<Integer> messageIds) {
        UserContext userContext = UserContextHolder.getRequiredContext();
        Integer currentUserId = userContext.getUserId();

        if (messageIds == null || messageIds.isEmpty()) {
            return;
        }
        chatMessageService.markClientMessagesAsRead(chatId, currentUserId, messageIds);
    }

}
