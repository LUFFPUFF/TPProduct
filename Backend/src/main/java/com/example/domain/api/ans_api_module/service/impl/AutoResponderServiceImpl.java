package com.example.domain.api.ans_api_module.service.impl;

import com.example.database.model.ai_module.AIResponses;
import com.example.database.model.chats_messages_module.chat.Chat;
import com.example.database.model.chats_messages_module.chat.ChatMessageSenderType;
import com.example.database.model.chats_messages_module.chat.ChatStatus;
import com.example.database.model.chats_messages_module.message.ChatMessage;
import com.example.database.repository.ai_module.AIResponsesRepository;
import com.example.domain.api.ans_api_module.answer_finder.dto.AnswerSearchResultItem;
import com.example.domain.api.ans_api_module.answer_finder.exception.AnswerSearchException;
import com.example.domain.api.ans_api_module.answer_finder.service.AnswerSearchService;
import com.example.domain.api.ans_api_module.engine.AutoResponderDecisionEngineImpl;
import com.example.domain.api.ans_api_module.engine.IAutoResponderDecisionEngine;
import com.example.domain.api.ans_api_module.generation.exception.MLException;
import com.example.domain.api.ans_api_module.generation.model.enums.GenerationType;
import com.example.domain.api.ans_api_module.event.AutoResponderEscalationEvent;
import com.example.domain.api.ans_api_module.exception.AutoResponderException;
import com.example.domain.api.ans_api_module.model.AutoResponderResult;
import com.example.domain.api.ans_api_module.service.IAutoResponderService;
import com.example.domain.api.ans_api_module.service.ai.IAIFeedbackService;
import com.example.domain.api.chat_service_api.exception_handler.exception.ExternalMessagingException;
import com.example.domain.api.chat_service_api.integration.service.IExternalMessagingService;
import com.example.domain.api.chat_service_api.mapper.ChatMessageMapper;
import com.example.domain.api.chat_service_api.model.dto.MessageDto;
import com.example.domain.api.chat_service_api.model.rest.mesage.SendMessageRequestDTO;
import com.example.domain.api.chat_service_api.service.IChatMessageService;
import com.example.domain.api.chat_service_api.service.chat.IChatQueryService;
import com.example.domain.api.chat_service_api.service.message.IChatMessageQueryService;
import com.example.domain.api.chat_service_api.util.MdcUtil;
import com.example.domain.security.model.UserContext;
import com.example.domain.security.util.UserContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AutoResponderServiceImpl implements IAutoResponderService {

    private final IAutoResponderDecisionEngine decisionEngine;
    private final IChatMessageService chatMessageService;
    private final IChatMessageQueryService chatMessageQueryService;
    private final IChatQueryService chatQueryService;
    private final ChatMessageMapper messageMapper;
    private final IExternalMessagingService externalMessagingService;
    private final ApplicationEventPublisher eventPublisher;
    private final MessageSource messageSource;
    private final IAIFeedbackService iaiFeedbackService;
    private final AIResponsesRepository aiResponsesRepository;

    private static final ChatMessageSenderType AUTO_RESPONDER_SENDER_TYPE = ChatMessageSenderType.AUTO_RESPONDER;

    private static final String KEY_OPERATION = "operation";
    private static final String KEY_CHAT_ID = "chatId";
    private static final String KEY_MESSAGE_ID = "messageId";

    private static final String DEFAULT_MSG_ESCALATE_NO_ANSWER = "Извините, я не смог найти или сгенерировать подходящий ответ. Передаю ваш вопрос оператору.";
    private static final String DEFAULT_MSG_ESCALATE_SEARCH_ERROR = "Извините, возникла проблема при поиске ответа. Передаю ваш вопрос оператору.";
    private static final String DEFAULT_MSG_ESCALATE_PROCESSING_ERROR = "Извините, произошла ошибка при обработке вашего запроса. Передаю ваш вопрос оператору.";
    private static final String DEFAULT_MSG_ESCALATE_GENERATION_ERROR = "К сожалению, не удалось автоматически подготовить ответ. Соединяю с оператором.";
    private static final String DEFAULT_MSG_ESCALATE_REWRITE_ERROR = "Не удалось адаптировать ответ для вас. Пожалуйста, подождите, оператор скоро подключится.";

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processNewPendingChat(Integer chatId) throws AutoResponderException {
        MdcUtil.withContext(
                () -> {
                    log.info("AutoResponder: Processing new pending chat ID {}", chatId);
                    Optional<Chat> chatOptional = chatQueryService.findChatEntityById(chatId);

                    if (chatOptional.isEmpty()) {
                        log.warn("AutoResponder: Chat ID {} not found for initial processing.", chatId);
                        return null;
                    }
                    Chat chat = chatOptional.get();

                    if (chat.getStatus() != ChatStatus.PENDING_AUTO_RESPONDER) {
                        log.info("AutoResponder: Chat ID {} is not PENDING_AUTO_RESPONDER (status: {}). Skipping initial processing.", chatId, chat.getStatus());
                        return null;
                    }

                    try {
                        Optional<ChatMessage> firstClientMessageOpt = chatQueryService.findFirstMessageEntityByChatId(chatId, null);
                        if (firstClientMessageOpt.isPresent()) {
                            MessageDto firstMessageDto = messageMapper.toDto(firstClientMessageOpt.get());
                            processIncomingMessageInternal(firstMessageDto, chat, null);
                        } else {
                            log.warn("AutoResponder: No first message found in new pending chat ID {}. Escalating as AR cannot process.", chatId);
                            escalateChat(chat, AutoResponderDecisionEngineImpl.MSG_KEY_ESCALATE_NO_ANSWER);
                        }
                    } catch (Exception e) {
                        log.error("Error during initial auto-responder processing for chat {}. Escalating.", chatId, e);
                        if (e instanceof AutoResponderException) throw (AutoResponderException) e;
                        throw new AutoResponderException("Error during initial auto-responder processing for chat " + chatId, e);
                    }
                    return null;
                },
                KEY_OPERATION, "processNewPendingChat",
                KEY_CHAT_ID, chatId
        );
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processIncomingMessage(MessageDto messageDTO, Chat chatInput) throws AutoResponderException {
        Chat chat = chatQueryService.findChatEntityById(chatInput.getId())
                .orElseThrow(() -> new AutoResponderException("Chat not found during processIncomingMessage: " + chatInput.getId()));

        MdcUtil.withContext(
                () -> {
                    String clientChatHistory = getClientChatHistoryForStyleAnalysis(chat.getId(), messageDTO.getId(), null);
                    processIncomingMessageInternal(messageDTO, chat, clientChatHistory);
                    return null;
                },
                KEY_OPERATION, "processIncomingMessage",
                KEY_CHAT_ID, chat.getId(),
                KEY_MESSAGE_ID, messageDTO.getId()
        );
    }

    private void processIncomingMessageInternal(MessageDto messageDTO, Chat chat, String clientChatHistoryForStyle) throws AutoResponderException {
        log.info("AutoResponder: Internal processing for message ID {} in chat ID {}. History for style provided: {}",
                messageDTO.getId(), chat.getId(), StringUtils.hasText(clientChatHistoryForStyle));

        if (chat.getStatus() != ChatStatus.PENDING_AUTO_RESPONDER || messageDTO.getSenderType() != ChatMessageSenderType.CLIENT) {
            log.debug("AutoResponder: Skipping message ID {} in chat ID {}. Status: {}, SenderType: {}",
                    messageDTO.getId(), chat.getId(), chat.getStatus(), messageDTO.getSenderType());
            return;
        }

        String clientQuery = messageDTO.getContent();
        Integer companyId = chat.getCompany() != null ? chat.getCompany().getId() : null;
        String companyDescription = (chat.getCompany() != null && chat.getCompany().getCompanyDescription() != null)
                ? chat.getCompany().getCompanyDescription() : null;

        if (!StringUtils.hasText(clientQuery)) {
            log.warn("AutoResponder: Received empty client query in message ID {}. Skipping.", messageDTO.getId());
            return;
        }

        AIResponses loggedAiResponseInThisCycle = null;

        try {
            AutoResponderResult result = decisionEngine.decideResponse(clientQuery, companyId, companyDescription, clientChatHistoryForStyle);

            if (result.isAnswerProvided() && StringUtils.hasText(result.getResponseText())) {
                ChatMessage aiChatMessage = sendAutoResponderMessageInternal(chat, result.getResponseText());
                if (aiChatMessage != null) {
                    if (result.isPurelyGenerated()) {
                        loggedAiResponseInThisCycle = iaiFeedbackService.logAiResponse(chat, chat.getClient(), aiChatMessage, result.getConfidence());
                    }
                }

            } else if (result.isRequiresEscalation()) {
                handleEscalation(chat, result.getEscalationReasonMessageKey(), messageDTO.getId());
            } else {
                log.warn("AutoResponder: Decision engine provided no answer and no explicit escalation for chat {}. Escalating by default.", chat.getId());
                handleEscalation(chat, AutoResponderDecisionEngineImpl.MSG_KEY_ESCALATE_NO_ANSWER, messageDTO.getId());
            }
        } catch (Exception e) {
            log.error("AutoResponder: Unexpected error from decision engine or subsequent processing for chat {}: {}. Escalating.",
                    chat.getId(), e.getMessage(), e);
            if (e instanceof AutoResponderException) throw (AutoResponderException) e;
            throw new AutoResponderException("Error in auto-responder processing for chat " + chat.getId(), e);
        }
        log.info("AutoResponder: Finished internal processing for message ID {} in chat ID {}",
                messageDTO.getId(), chat.getId());
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void stopForChat(Integer chatId) {
        MdcUtil.withContext(
                () -> {
                    Optional<Chat> chatOptional = chatQueryService.findChatEntityById(chatId);
                    if (chatOptional.isPresent()) {
                        Chat chat = chatOptional.get();
                        if (chat.getStatus() == ChatStatus.PENDING_AUTO_RESPONDER) {
                            log.info("AutoResponder: Stop requested for chat ID {} which was PENDING_AUTO_RESPONDER. Publishing escalation event.", chatId);
                            if (chat.getClient() != null) {
                                eventPublisher.publishEvent(new AutoResponderEscalationEvent(this, chat.getId(), chat.getClient().getId()));
                                log.info("AutoResponder: Escalation event published for chat ID {} due to explicit stop request.", chatId);
                            } else {
                                log.warn("AutoResponder: Cannot publish escalation event on stop for chat {} - client info missing.", chatId);
                            }
                        } else {
                            log.info("AutoResponder: Stop request for chat ID {} which is not PENDING_AUTO_RESPONDER (current: {}). No event published by AR.", chatId, chat.getStatus());
                        }
                    }  else {
                        log.warn("AutoResponder: Stop request for non-existent chat ID {}.", chatId);
                    }
                    return null;
                },
                KEY_OPERATION, "stopForChat",
                KEY_CHAT_ID, chatId
        );
    }

    private void handleEscalation(Chat chatInput, String reasonMessageKey, Integer originatingClientMessageId) {
        Chat currentChatState = chatQueryService.findChatEntityById(chatInput.getId()).orElse(chatInput);
        if (currentChatState.getStatus() == ChatStatus.PENDING_AUTO_RESPONDER) {
            log.info("AutoResponder escalating chat {}. Feedback request will be handled by operator assignment process if applicable.", currentChatState.getId());
            escalateChat(currentChatState, reasonMessageKey);
        } else {
            log.info("AutoResponder: Chat {} was already handled (status: {}) before escalation for client message {} could complete.",
                    currentChatState.getId(), currentChatState.getStatus(), originatingClientMessageId);
        }
    }

    private String getClientChatHistoryForStyleAnalysis(Integer chatId, Integer excludeMessageId, UserContext userContextForHistory) {
        final int HISTORY_LIMIT = 3;
        try {

            List<MessageDto> recentMessages = chatMessageQueryService.getRecentClientMessages(chatId, HISTORY_LIMIT, excludeMessageId, userContextForHistory);
            if (recentMessages.isEmpty()) {
                return null;
            }
            return recentMessages.stream()
                    .map(MessageDto::getContent)
                    .filter(content -> content != null && !content.trim().isEmpty())
                    .collect(Collectors.joining("\n---\n"));
        } catch (Exception e) {
            log.warn("AutoResponder: Failed to retrieve client chat history for style analysis for chat {}. Error: {}", chatId, e.getMessage());
            return null;
        }
    }

    public Integer findLastLoggedAiResponseIdForChat(Integer chatId) {
        if (chatId == null) {
            log.warn("findLastLoggedAiResponseIdForChat called with null chatId.");
            return null;
        }
        Optional<AIResponses> responseOpt = aiResponsesRepository.findTopByChatIdOrderByCreatedAtDesc(chatId);
        if (responseOpt.isPresent()) {
            log.info("Found last logged AI response ID {} for chat {}", responseOpt.get().getId(), chatId);
            return responseOpt.get().getId();
        } else {
            log.info("No logged AI responses found for chat {}", chatId);
            return null;
        }
    }

    private String getEscalationMessage(String messageKey) {
        String defaultMessage = DEFAULT_MSG_ESCALATE_PROCESSING_ERROR;
        defaultMessage = switch (messageKey) {
            case AutoResponderDecisionEngineImpl.MSG_KEY_ESCALATE_NO_ANSWER -> DEFAULT_MSG_ESCALATE_NO_ANSWER;
            case AutoResponderDecisionEngineImpl.MSG_KEY_ESCALATE_SEARCH_ERROR -> DEFAULT_MSG_ESCALATE_SEARCH_ERROR;
            case AutoResponderDecisionEngineImpl.MSG_KEY_ESCALATE_GENERATION_ERROR ->
                    DEFAULT_MSG_ESCALATE_GENERATION_ERROR;
            case AutoResponderDecisionEngineImpl.MSG_KEY_ESCALATE_REWRITE_ERROR -> DEFAULT_MSG_ESCALATE_REWRITE_ERROR;
            default -> defaultMessage;
        };

        try {
            return messageSource.getMessage(messageKey, null, Locale.getDefault());
        } catch (Exception e) {
            log.warn("Could not resolve escalation message for key '{}', using default: \"{}\"", messageKey, defaultMessage);
            return defaultMessage;
        }
    }

    private ChatMessage sendAutoResponderMessageInternal(Chat chat, String content) {
        if (chat.getClient() == null) {
            log.error("AutoResponder: Cannot send message for chat {} because client is null.", chat.getId());
            return null;
        }

        SendMessageRequestDTO messageRequest = new SendMessageRequestDTO();
        messageRequest.setChatId(chat.getId());
        messageRequest.setContent(content);
        messageRequest.setSenderId(chat.getClient().getId());
        messageRequest.setSenderType(AUTO_RESPONDER_SENDER_TYPE);

        MessageDto savedMessageDto;
        ChatMessage savedMessageEntity;

        try {
            savedMessageDto = chatMessageService.processAndSaveMessage(
                    messageRequest,
                    messageRequest.getSenderId(),
                    messageRequest.getSenderType()
            );
            if (savedMessageDto != null && savedMessageDto.getId() != null) {
                Optional<ChatMessage> messageEntityOpt = chatMessageQueryService.findMessageEntityById(savedMessageDto.getId(), null);
                if (messageEntityOpt.isPresent()) {
                    savedMessageEntity = messageEntityOpt.get();
                    log.info("AutoResponder: System message ID {} (external sender type {}) saved internally for chat ID {}.",
                            savedMessageEntity.getId(), savedMessageEntity.getSenderType(), chat.getId());
                } else {
                    log.error("AutoResponder: CRITICAL - Saved message DTO ID {} but could not retrieve ChatMessage entity.", savedMessageDto.getId());

                    return null;
                }
            } else {
                log.error("AutoResponder: Failed to save system message internally (DTO or ID is null) for chat {}.", chat.getId());
                return null;
            }
        } catch (Exception e) {
            log.error("AutoResponder: Failed to save system message internally for chat {}: {}", chat.getId(), e.getMessage(), e);
            return null;
        }

        if (StringUtils.hasText(content)) {
            try {
                externalMessagingService.sendMessageToExternal(chat.getId(), content);
                log.info("AutoResponder: Message ID {} successfully placed for external sending for chat ID {}.", savedMessageEntity.getId(), chat.getId());
            } catch (ExternalMessagingException e) {
                log.error("AutoResponder: Failed to send message ID {} to external channel for chat {}: {}", savedMessageEntity.getId(), chat.getId(), e.getMessage(), e);
            } catch (Exception e) {
                log.error("AutoResponder: Unexpected error sending message ID {} to external channel for chat {}: {}", savedMessageEntity.getId(), chat.getId(), e.getMessage(), e);
            }
        }

        return savedMessageEntity;
    }

    private void escalateChat(Chat chat, String reasonMessageKey) {
        log.warn("AutoResponder: Publishing escalation event for chat ID {} (reason key: {})...", chat.getId(), reasonMessageKey);
        if (chat.getClient() != null) {
            eventPublisher.publishEvent(new AutoResponderEscalationEvent(this, chat.getId(), chat.getClient().getId()));
            log.info("AutoResponder: Escalation event published for chat ID {}. Feedback request (if applicable) should have been initiated.", chat.getId());
        } else {
            log.error("AutoResponder: Cannot escalate chat {} - client information missing.", chat.getId());

        }
    }
}
