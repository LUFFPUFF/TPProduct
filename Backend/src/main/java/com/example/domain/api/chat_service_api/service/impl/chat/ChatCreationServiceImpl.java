package com.example.domain.api.chat_service_api.service.impl.chat;

import com.example.database.model.chats_messages_module.chat.Chat;
import com.example.database.model.chats_messages_module.chat.ChatChannel;
import com.example.database.model.chats_messages_module.chat.ChatMessageSenderType;
import com.example.database.model.company_subscription_module.company.Company;
import com.example.database.model.company_subscription_module.user_roles.user.User;
import com.example.database.model.crm_module.client.Client;
import com.example.database.repository.chats_messages_module.ChatRepository;
import com.example.domain.api.ans_api_module.exception.AutoResponderException;
import com.example.domain.api.ans_api_module.service.IAutoResponderService;
import com.example.domain.api.chat_service_api.event.chat.ChatAssignedToOperatorEvent;
import com.example.domain.api.chat_service_api.event.chat.ChatStatusChangedEvent;
import com.example.domain.api.chat_service_api.event.chat.NewPendingChatEvent;
import com.example.domain.api.chat_service_api.exception_handler.ResourceNotFoundException;
import com.example.domain.api.chat_service_api.exception_handler.exception.service.ChatServiceException;
import com.example.domain.api.chat_service_api.mapper.ChatMapper;
import com.example.domain.api.chat_service_api.model.dto.ChatDTO;
import com.example.domain.api.chat_service_api.model.dto.ChatDetailsDTO;
import com.example.domain.api.chat_service_api.model.rest.chat.CreateChatRequestDTO;
import com.example.domain.api.company_module.service.IClientService;
import com.example.domain.api.company_module.service.IUserService;
import com.example.domain.api.chat_service_api.service.chat.IChatCreationService;
import com.example.domain.api.chat_service_api.util.ChatFactory;
import com.example.domain.api.chat_service_api.util.ChatInitialMessageHelper;
import com.example.domain.api.chat_service_api.util.ChatMetricHelper;
import com.example.domain.api.chat_service_api.util.MdcUtil;
import com.example.domain.api.statistics_module.aop.annotation.Counter;
import com.example.domain.api.statistics_module.aop.annotation.MeteredOperation;
import com.example.domain.api.statistics_module.aop.annotation.Tag;
import com.example.domain.security.model.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.file.AccessDeniedException;
import java.util.Objects;
import java.util.Optional;

import static com.example.domain.api.chat_service_api.service.impl.LeastBusyAssignmentService.OPEN_CHAT_STATUSES;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatCreationServiceImpl implements IChatCreationService {

    private final ChatRepository chatRepository;
    private final IClientService clientService;
    private final IUserService userService;
    private final ChatMapper chatMapper;
    private final IAutoResponderService autoResponderService;
    private final ApplicationEventPublisher eventPublisher;
    private final ChatFactory chatFactory;
    private final ChatInitialMessageHelper initialMessageHelper;
    private final ChatMetricHelper metricHelper;

    private static final String KEY_COMPANY_ID_MDC = "companyIdMdc";
    private static final String KEY_CHAT_ID_MDC = "chatIdMdc";
    private static final String KEY_CLIENT_ID = "clientId";
    private static final String KEY_OPERATOR_ID = "operatorId";
    private static final String KEY_CHAT_CHANNEL = "chatChannel";

    private static final String OPERATION_CREATE_CHAT_FROM_CLIENT = "createChatFromClient";
    private static final String OPERATION_CREATE_CHAT_FROM_OPERATOR_UI = "createChatFromOperatorUI";
    private static final String OPERATION_CREATE_TEST_CHAT = "createTestChatForCurrentUser";

    private static final ChatChannel TEST_CHAT_CHANNEL = ChatChannel.Test;
    private static final String TEST_CHAT_INITIAL_MESSAGE = "Добрый день! Я тестовый клиент.";


    @Override
    @Transactional
    @MeteredOperation(
            counters = {
                    @Counter(
                            name = "chat_app_chats_created_total",
                            tags = {
                                    @Tag(key = "company_id", valueSpEL = "#result.companyId ?: 'unknown'"),
                                    @Tag(key = "channel", valueSpEL = "#result.chatChannel?.name() ?: 'UNKNOWN'"),
                                    @Tag(key = "from_operator_ui", valueSpEL = "'false'")
                            }
                    ),
                    @Counter(
                            name = "chat_app_chats_auto_responder_handled_total",
                            conditionSpEL = "#result.status?.name() == 'PENDING_AUTO_RESPONDER'",
                            tags = {
                                    @Tag(key = "company_id", valueSpEL = "#result.companyId ?: 'unknown'"),
                                    @Tag(key = "channel", valueSpEL = "#result.chatChannel?.name() ?: 'UNKNOWN'")
                            }
                    )
            }
    )
    public ChatDetailsDTO createChat(CreateChatRequestDTO createRequest) {
        return MdcUtil.withContext(
                () -> {
                    log.info("Attempting to create chat for client ID: {}", createRequest.getClientId());

                    Client client = findClientAndValidateCompany(createRequest.getClientId(), OPERATION_CREATE_CHAT_FROM_CLIENT);
                    Company company = client.getCompany();
                    MDC.put(KEY_COMPANY_ID_MDC, metricHelper.getCompanyIdStr(company));

                    Optional<Chat> existingChatOpt = findOpenChatByClientAndChannel(client.getId(), createRequest.getChatChannel());
                    if (existingChatOpt.isPresent()) {
                        return handleExistingChat(existingChatOpt.get(), "Client");
                    }

                    Chat chat = chatFactory.createClientInitiatedChat(client, company, createRequest);
                    Chat savedChat = persistChat(chat, OPERATION_CREATE_CHAT_FROM_CLIENT, company);
                    MDC.put(KEY_CHAT_ID_MDC, String.valueOf(savedChat.getId()));

                    savedChat = initialMessageHelper.sendInitialMessageAndUpdateChat(
                            savedChat, client, createRequest.getInitialMessageContent(), ChatMessageSenderType.CLIENT);

                    processWithAutoResponder(savedChat, company);
                    publishNewPendingChatEvent(savedChat);

                    return chatMapper.toDetailsDto(savedChat);
                },
                "operation", OPERATION_CREATE_CHAT_FROM_CLIENT,
                KEY_CLIENT_ID, createRequest.getClientId(),
                KEY_CHAT_CHANNEL, createRequest.getChatChannel() != null ? createRequest.getChatChannel().name() : "null"
        );
    }

    @Override
    @Transactional
    @MeteredOperation(
            counters = {
                    @Counter(name = "chat_app_chats_created_total",
                            tags = { @Tag(key = "company_id", valueSpEL = "#result.companyId ?: 'unknown'"),
                                    @Tag(key = "channel", valueSpEL = "#result.chatChannel?.name() ?: 'UNKNOWN'"),
                                    @Tag(key = "from_operator_ui", valueSpEL = "'true'") }),
                    @Counter(name = "chat_app_chats_assigned_total",
                            tags = { @Tag(key = "company_id", valueSpEL = "#result.companyId ?: 'unknown'"),
                                    @Tag(key = "channel", valueSpEL = "#result.chatChannel?.name() ?: 'UNKNOWN'"),
                                    @Tag(key = "auto_assigned", valueSpEL = "'false'") }),
                    @Counter(name = "chat_app_chats_operator_linked_total",
                            tags = { @Tag(key = "company_id", valueSpEL = "#result.companyId ?: 'unknown'"),
                                    @Tag(key = "channel", valueSpEL = "#result.chatChannel?.name() ?: 'UNKNOWN'") })
            }
    )
    public ChatDetailsDTO createChatFromOperatorUI(CreateChatRequestDTO createRequest, UserContext userContext) throws AccessDeniedException {
        return MdcUtil.withContext(
                () -> {
                    log.info("Operator {} attempting to create chat for client {}", userContext.getUserId(), createRequest.getClientId());

                    User operator = findOperatorAndValidateCompany(userContext.getUserId(), OPERATION_CREATE_CHAT_FROM_OPERATOR_UI);
                    Client client = findClientAndValidateCompany(createRequest.getClientId(), OPERATION_CREATE_CHAT_FROM_OPERATOR_UI);
                    MDC.put(KEY_COMPANY_ID_MDC, metricHelper.getCompanyIdStr(operator.getCompany()));

                    validateOperatorClientCompanyMatch(operator, client);

                    Optional<Chat> existingChatOpt = findOpenChatByClientAndChannel(client.getId(), createRequest.getChatChannel());
                    if (existingChatOpt.isPresent()) {
                        return handleExistingChat(existingChatOpt.get(), "OperatorUI");
                    }

                    Chat chat = chatFactory.createOperatorInitiatedChat(client, operator, createRequest.getChatChannel());
                    Chat savedChat = persistChat(chat, OPERATION_CREATE_CHAT_FROM_OPERATOR_UI, operator.getCompany());
                    MDC.put(KEY_CHAT_ID_MDC, String.valueOf(savedChat.getId()));

                    metricHelper.recordChatAssignmentTimeIfApplicable(savedChat);

                    if (StringUtils.hasText(createRequest.getInitialMessageContent())) {
                        savedChat = initialMessageHelper.sendInitialMessageFromOperator(savedChat, operator, createRequest.getInitialMessageContent());
                    }

                    publishChatAssignedAndStatusChangedEvents(savedChat, operator);
                    return chatMapper.toDetailsDto(savedChat);
                },
                "operation", OPERATION_CREATE_CHAT_FROM_OPERATOR_UI,
                KEY_OPERATOR_ID, userContext.getUserId(),
                KEY_CLIENT_ID, createRequest.getClientId(),
                KEY_CHAT_CHANNEL, createRequest.getChatChannel() != null ? createRequest.getChatChannel().name() : "null"
        );
    }

    @Override
    @Transactional
    public ChatDetailsDTO createTestChatForCurrentUser(UserContext userContext) throws AccessDeniedException {
        return MdcUtil.withContext(
                () -> {
                    log.info("Operator {} attempting to create a test chat.", userContext.getUserId());

                    User operator = findOperatorAndValidateCompany(userContext.getUserId(), OPERATION_CREATE_TEST_CHAT);
                    Company operatorCompany = operator.getCompany();
                    MDC.put(KEY_COMPANY_ID_MDC, metricHelper.getCompanyIdStr(operatorCompany));

                    Client testClient = clientService.findOrCreateTestClientForOperator(operator);
                    MDC.put(KEY_CLIENT_ID, String.valueOf(testClient.getId()));


                    Optional<Chat> existingOpenTestChat = chatRepository.findFirstByClientIdAndCompanyIdAndChatChannelAndStatusInOrderByCreatedAtDesc(
                            testClient.getId(), operatorCompany.getId(), TEST_CHAT_CHANNEL, OPEN_CHAT_STATUSES);

                    if (existingOpenTestChat.isPresent()) {
                        return handleExistingChat(existingOpenTestChat.get(), "TestChat");
                    }

                    Chat chat = chatFactory.createOperatorInitiatedChat(testClient, operator, TEST_CHAT_CHANNEL);
                    Chat savedChat = persistChat(chat, OPERATION_CREATE_TEST_CHAT, operatorCompany);
                    MDC.put(KEY_CHAT_ID_MDC, String.valueOf(savedChat.getId()));

                    savedChat = initialMessageHelper.sendInitialMessageAndUpdateChat(
                            savedChat, testClient, TEST_CHAT_INITIAL_MESSAGE, ChatMessageSenderType.CLIENT);

                    metricHelper.recordChatAssignmentTimeIfApplicable(savedChat);
                    publishChatAssignedAndStatusChangedEvents(savedChat, operator);
                    return chatMapper.toDetailsDto(savedChat);
                },
                "operation", OPERATION_CREATE_TEST_CHAT,
                KEY_OPERATOR_ID, userContext.getUserId()
        );
    }

    private Client findClientAndValidateCompany(Integer clientId, String operationName) {
        Client client = clientService.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client with ID " + clientId + " not found."));
        if (client.getCompany() == null) {
            log.error("Client with ID {} is not associated with a company for operation {}.", clientId, operationName);
            metricHelper.incrementChatOperationError(operationName, (Company) null, "ClientNotAssociatedWithCompany");
            throw new ResourceNotFoundException("Client is not associated with a company.");
        }
        return client;
    }

    private User findOperatorAndValidateCompany(Integer operatorId, String operationName) throws AccessDeniedException {
        User operator = userService.findById(operatorId)
                .orElseThrow(() -> new ResourceNotFoundException("Operator with ID " + operatorId + " not found."));
        if (operator.getCompany() == null) {
            log.warn("Operator {} is not associated with a company for operation {}.", operatorId, operationName);
            metricHelper.incrementChatOperationError(operationName, (Company) null, "OperatorNotAssociatedWithCompany");
            throw new AccessDeniedException("Authenticated user is not associated with a company.");
        }
        return operator;
    }

    private void validateOperatorClientCompanyMatch(User operator, Client client) throws AccessDeniedException {
        if (!Objects.equals(operator.getCompany().getId(), client.getCompany().getId())) {
            log.warn("Operator ID {} from company {} attempted to interact with client ID {} from different company {} (Operation: Chat Creation)",
                    operator.getId(), operator.getCompany().getId(), client.getId(), client.getCompany().getId());
            throw new AccessDeniedException("Access Denied: Operator and client must belong to the same company.");
        }
    }

    private ChatDetailsDTO handleExistingChat(Chat existingChat, String creationContext) {
        log.warn("[{}] Open chat ID {} already exists for client {} on channel {}. Returning existing.",
                creationContext, existingChat.getId(), existingChat.getClient().getId(), existingChat.getChatChannel());
        return chatMapper.toDetailsDto(existingChat);
    }

    private Chat persistChat(Chat chat, String operationName, Company companyContext) {
        try {
            Chat savedChat = chatRepository.save(chat);
            log.info("[{}] Saved initial chat entity with ID: {}", operationName, savedChat.getId());
            MDC.put("chatId", String.valueOf(savedChat.getId()));
            return savedChat;
        } catch (Exception e) {
            metricHelper.incrementChatOperationError(operationName, companyContext, "ChatPersistenceError");
            log.error("[{}] Error persisting chat for company {}: {}", operationName, metricHelper.getCompanyIdStr(companyContext), e.getMessage(), e);
            throw new ChatServiceException("Failed to save chat information.", e);
        }
    }

    private Optional<Chat> findOpenChatByClientAndChannel(Integer clientId, ChatChannel channel) {
        return chatRepository
                .findFirstByClientIdAndChatChannelAndStatusInOrderByCreatedAtDesc(clientId, channel, OPEN_CHAT_STATUSES);
    }

    private void processWithAutoResponder(Chat chat, Company company) {
        try {
            autoResponderService.processNewPendingChat(chat.getId());
            log.info("Auto-responder triggered for chat ID: {}", chat.getId());
        } catch (AutoResponderException e) {
            log.error("Failed to trigger auto-responder for chat ID {}: {}", chat.getId(), e.getMessage(), e);
            metricHelper.incrementChatOperationError("createChatFromClient_AutoResponder", company, e.getClass().getSimpleName());
            throw e;
        }
    }

    private void publishNewPendingChatEvent(Chat chat) {
        ChatDTO chatDto = chatMapper.toDto(chat);
        eventPublisher.publishEvent(new NewPendingChatEvent(this, chatDto, chat.getCompany().getId()));
        log.debug("Published NewPendingChatEvent for chat ID {}", chat.getId());
    }

    private void publishChatAssignedAndStatusChangedEvents(Chat chat, User operator) {
        ChatDTO chatDto = chatMapper.toDto(chat);
        eventPublisher.publishEvent(new ChatAssignedToOperatorEvent(this, chatDto, operator.getEmail(), operator.getCompany().getId()));
        eventPublisher.publishEvent(new ChatStatusChangedEvent(this, chat.getId(), chatDto));
        log.debug("Published ChatAssignedToOperatorEvent and ChatStatusChangedEvent for operator-initiated chat ID {}", chat.getId());
    }
}
