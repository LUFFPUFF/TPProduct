package com.example.domain.api.chat_service_api.service.impl.message;

import com.example.database.model.chats_messages_module.chat.Chat;
import com.example.database.model.chats_messages_module.chat.ChatChannel;
import com.example.database.model.chats_messages_module.chat.ChatMessageSenderType;
import com.example.database.model.chats_messages_module.chat.ChatStatus;
import com.example.database.model.chats_messages_module.message.ChatMessage;
import com.example.database.model.company_subscription_module.company.Company;
import com.example.database.model.company_subscription_module.user_roles.user.User;
import com.example.database.model.crm_module.client.Client;
import com.example.database.repository.chats_messages_module.ChatMessageRepository;
import com.example.database.repository.chats_messages_module.ChatRepository;
import com.example.database.repository.company_subscription_module.UserRepository;
import com.example.database.repository.crm_module.ClientRepository;
import com.example.domain.api.chat_service_api.event.chat.ChatStatusChangedEvent;
import com.example.domain.api.chat_service_api.event.message.ChatMessageSentEvent;
import com.example.domain.api.chat_service_api.exception_handler.ChatNotFoundException;
import com.example.domain.api.chat_service_api.exception_handler.ResourceNotFoundException;
import com.example.domain.api.chat_service_api.exception_handler.exception.ExternalMessagingException;
import com.example.domain.api.chat_service_api.exception_handler.exception.service.ChatServiceException;
import com.example.domain.api.chat_service_api.integration.manager.widget.model.SenderInfoWidgetChat;
import com.example.domain.api.chat_service_api.integration.service.IExternalMessagingService;
import com.example.domain.api.chat_service_api.mapper.ChatMapper;
import com.example.domain.api.chat_service_api.mapper.ChatMessageMapper;
import com.example.domain.api.chat_service_api.model.dto.MessageDto;
import com.example.domain.api.chat_service_api.model.rest.mesage.SendMessageRequestDTO;
import com.example.domain.api.chat_service_api.service.message.IChatMessageCreationService;
import com.example.domain.api.chat_service_api.service.message.IOperatorChatInteractionService;
import com.example.domain.api.chat_service_api.util.ChatMessageFactory;
import com.example.domain.api.chat_service_api.util.ChatMetricHelper;
import com.example.domain.api.chat_service_api.util.MdcUtil;
import com.example.domain.api.statistics_module.metrics.service.IChatMetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatMessageCreationServiceImpl implements IChatMessageCreationService {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatRepository chatRepository;
    private final UserRepository userRepository;
    private final ClientRepository clientRepository;
    private final ChatMessageMapper chatMessageMapper;
    private final IExternalMessagingService externalMessagingService;
    private final ApplicationEventPublisher eventPublisher;
    private final IOperatorChatInteractionService operatorChatInteractionService;

    private final ChatMapper chatMapper;
    private final IChatMetricsService chatMetricsService;
    private final ChatMessageFactory chatMessageFactory;
    private final ChatMetricHelper chatMetricHelper;

    private static final String OPERATION_PROCESS_MESSAGE = "processAndSaveMessage";
    private static final String KEY_CHAT_ID = "chatId";
    private static final String KEY_SENDER_ID = "senderId";
    private static final String KEY_SENDER_TYPE = "senderType";
    private static final String KEY_COMPANY_ID_MDC = "companyIdMdc";

    private record SenderInfo(User operator, Client client) {}

    @Override
    @Transactional
    public MessageDto processAndSaveMessage(SendMessageRequestDTO messageRequest, Integer senderId, ChatMessageSenderType senderType) {
        return MdcUtil.withContext(
                () -> {
                    log.info("Processing message for chat ID {}, sender ID {}, type {}",
                            messageRequest.getChatId(), senderId, senderType);

                    Chat chat = chatRepository.findById(messageRequest.getChatId())
                            .orElseThrow(() -> new ChatNotFoundException("Chat with ID " + messageRequest.getChatId() + " not found for message processing."));

                    MDC.put(KEY_COMPANY_ID_MDC, chatMetricHelper.getCompanyIdStr(chat.getCompany()));
                    MDC.put(KEY_CHAT_ID, String.valueOf(chat.getId()));

                    SenderInfo senderInfo = determineSender(senderId, senderType);

                    ChatMessage message = chatMessageFactory.createMessageEntity(messageRequest, chat, senderInfo.operator(), senderInfo.client(), senderType);

                    ChatMessage savedMessage = persistMessage(message, chat.getCompany());

                    updateChatAfterMessage(chat, savedMessage, senderType);

                    chat.setLastMessageAt(savedMessage.getSentAt());

                    boolean chatStateChangedByInteraction = false;
                    if (senderType == ChatMessageSenderType.OPERATOR && senderInfo.operator() != null) {

                        chatStateChangedByInteraction = operatorChatInteractionService.processOperatorMessageImpact(chat, senderInfo.operator(), savedMessage);
                    } else {

                        chatRepository.save(chat);
                    }

                    publishMessageEvents(savedMessage, chatRepository.findById(chat.getId()).orElse(chat), chatStateChangedByInteraction);

                    if (senderType == ChatMessageSenderType.OPERATOR && senderInfo.operator() != null) {
                        User operator = senderInfo.operator();
                        String operatorDisplayName = operator.getEmail();

                        if (operatorDisplayName == null || operatorDisplayName.isBlank()) {
                            operatorDisplayName = "Оператор " + operator.getId();
                        }

                        SenderInfoWidgetChat senderInfoWidgetChat = SenderInfoWidgetChat.builder()
                                .senderType(ChatMessageSenderType.OPERATOR)
                                .id(String.valueOf(operator.getId()))
                                .displayName(operatorDisplayName)
                                .build();

                        sendToExternalService(chat, savedMessage.getContent(), senderInfoWidgetChat);
                    }

                    return chatMessageMapper.toDto(savedMessage);
                },
                "operation", OPERATION_PROCESS_MESSAGE,
                KEY_CHAT_ID, messageRequest.getChatId(),
                KEY_SENDER_ID, senderId,
                KEY_SENDER_TYPE, senderType.name()
        );
    }

    private SenderInfo determineSender(Integer senderId, ChatMessageSenderType senderType) {
        User operator = null;
        Client client = null;

        switch (senderType) {
            case OPERATOR:
                operator = userRepository.findById(senderId)
                        .orElseThrow(() -> new ResourceNotFoundException("Operator with ID " + senderId + " not found."));
                break;
            case CLIENT:
                client = clientRepository.findById(senderId)
                        .orElseThrow(() -> new ResourceNotFoundException("Client with ID " + senderId + " not found."));
                break;
            case AUTO_RESPONDER:
                client = clientRepository.findById(senderId)
                        .orElseThrow(() -> new ResourceNotFoundException("Client ID " + senderId + " for AutoResponder not found."));
                break;
            default:
                log.error("Unsupported sender type received: {}", senderType);
                throw new IllegalArgumentException("Unsupported sender type: " + senderType);
        }
        return new SenderInfo(operator, client);
    }

    private boolean checkAndUpdateChatStatusAndOperator(Chat chat, User operator) {
        boolean statusChanged = false;
        if (chat.getUser() == null || !Objects.equals(chat.getUser().getId(), operator.getId())) {
            chat.setUser(operator);
            if (chat.getAssignedAt() == null ||
                    (chat.getStatus() != ChatStatus.ASSIGNED && chat.getStatus() != ChatStatus.IN_PROGRESS)) {
                chat.setAssignedAt(LocalDateTime.now());
            }
            if (chat.getStatus() == ChatStatus.PENDING_OPERATOR) statusChanged = true;
        }

        if (chat.getStatus() == ChatStatus.ASSIGNED || chat.getStatus() == ChatStatus.PENDING_OPERATOR) {
            chat.setStatus(ChatStatus.IN_PROGRESS);
            statusChanged = true;
            log.info("Chat {} status updated to IN_PROGRESS by operator {}'s message.", chat.getId(), operator.getId());
        }


        return statusChanged;
    }

    private ChatMessage persistMessage(ChatMessage message, Company company) {
        try {
            return chatMessageRepository.save(message);
        } catch (Exception e) {
            chatMetricHelper.incrementChatOperationError(OPERATION_PROCESS_MESSAGE + "_PersistFail",
                    company, e.getClass().getSimpleName());
            log.error("Error persisting chat message for chat {}: {}", message.getChat().getId(), e.getMessage(), e);
            throw new ChatServiceException("Failed to save chat message.", e);
        }
    }

    private void updateChatAfterMessage(Chat chat, ChatMessage savedMessage, ChatMessageSenderType senderType) {
        chat.setLastMessageAt(savedMessage.getSentAt());

        if (senderType == ChatMessageSenderType.OPERATOR &&
                chat.getAssignedAt() != null &&
                (chat.getStatus() == ChatStatus.ASSIGNED || chat.getStatus() == ChatStatus.IN_PROGRESS) &&
                !Boolean.TRUE.equals(chat.getHasOperatorResponded())) {

            if (!savedMessage.getSentAt().isBefore(chat.getAssignedAt())) {
                Duration firstResponseTime = Duration.between(chat.getAssignedAt(), savedMessage.getSentAt());

                chatMetricsService.recordChatFirstOperatorResponseTime(
                        chatMetricHelper.getCompanyIdStr(chat.getCompany()),
                        chat.getChatChannel() != null ? chat.getChatChannel() : ChatChannel.UNKNOWN,
                        firstResponseTime
                );
                chat.setHasOperatorResponded(true);
            } else {
                log.warn("Operator message sentAt ({}) is before chat assignedAt ({}) for chat {}. First response time logic skipped.",
                        savedMessage.getSentAt(), chat.getAssignedAt(), chat.getId());
            }
        }

        chatRepository.save(chat);
    }

    private void recordMessageMetrics(Chat chat, ChatMessage savedMessage, ChatMessageSenderType senderType) {
        String companyIdStr = chatMetricHelper.getCompanyIdStr(chat.getCompany());
        ChatChannel channel = chat.getChatChannel() != null ? chat.getChatChannel() : ChatChannel.UNKNOWN;

        chatMetricsService.incrementMessagesSent(companyIdStr, channel, senderType);

        if (savedMessage.getContent() != null) {
            chatMetricsService.recordMessageContentLength(
                    companyIdStr, channel, senderType, savedMessage.getContent().length()
            );
        }
    }

    private void publishMessageEvents(ChatMessage savedMessage, Chat chat, boolean statusExplicitlyChangedByInteraction) {
        MessageDto savedMessageDTO = chatMessageMapper.toDto(savedMessage);
        eventPublisher.publishEvent(new ChatMessageSentEvent(this, savedMessageDTO));
        log.debug("Published ChatMessageSentEvent for message ID {}", savedMessage.getId());

        if (statusExplicitlyChangedByInteraction) {
            eventPublisher.publishEvent(new ChatStatusChangedEvent(this, chat.getId(), chatMapper.toDto(chat)));
            log.debug("Published ChatStatusChangedEvent for chat ID {} after message {}", chat.getId(), savedMessage.getId());
        }
    }

    private void sendToExternalService(Chat chat, String content, SenderInfoWidgetChat senderInfoWidgetChat) {
        if (content != null && !content.trim().isEmpty()) {
            try {
                externalMessagingService.sendMessageToExternal(chat.getId(), content, senderInfoWidgetChat);
                log.info("Operator message for chat ID {} successfully placed for external sending.", chat.getId());
            } catch (ExternalMessagingException e) {
                chatMetricHelper.incrementChatOperationError(OPERATION_PROCESS_MESSAGE + "_ExternalSendFail",
                        chat.getCompany(), e.getClass().getSimpleName());
            }
        }
    }
}
