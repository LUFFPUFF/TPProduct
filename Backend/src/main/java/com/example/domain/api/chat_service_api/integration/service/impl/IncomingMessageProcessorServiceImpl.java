package com.example.domain.api.chat_service_api.integration.service.impl;

import com.example.database.model.chats_messages_module.chat.Chat;
import com.example.database.model.chats_messages_module.chat.ChatChannel;
import com.example.database.model.chats_messages_module.chat.ChatMessageSenderType;
import com.example.database.model.chats_messages_module.chat.ChatStatus;
import com.example.database.model.crm_module.client.Client;
import com.example.database.repository.chats_messages_module.ChatRepository;
import com.example.domain.api.ans_api_module.service.IAutoResponderService;
import com.example.domain.api.chat_service_api.exception_handler.ChatNotFoundException;
import com.example.domain.api.chat_service_api.integration.dto.IncomingChannelMessage;
import com.example.domain.api.chat_service_api.integration.service.IIncomingMessageProcessorService;
import com.example.domain.api.chat_service_api.model.dto.ChatDetailsDTO;
import com.example.domain.api.chat_service_api.model.dto.MessageDto;
import com.example.domain.api.chat_service_api.model.rest.chat.CreateChatRequestDTO;
import com.example.domain.api.chat_service_api.model.rest.mesage.SendMessageRequestDTO;
import com.example.domain.api.chat_service_api.service.IAssignmentStrategyService;
import com.example.domain.api.chat_service_api.service.IChatMessageService;
import com.example.domain.api.chat_service_api.service.chat.IChatService;
import com.example.domain.api.chat_service_api.util.ChatMetricHelper;
import com.example.domain.api.chat_service_api.util.MdcUtil;
import com.example.domain.api.company_module.service.IClientService;
import com.example.domain.api.statistics_module.metrics.service.IChatMetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class IncomingMessageProcessorServiceImpl implements IIncomingMessageProcessorService {

    private final IClientService clientService;
    private final IChatService chatServiceFacade;
    private final IChatMessageService chatMessageServiceFacade;
    private final IAutoResponderService autoResponderService;
    private final ChatRepository chatRepository;
    private final IChatMetricsService chatMetricsService;
    private final ChatMetricHelper chatMetricHelper;

    private static final int MAX_CONTENT_LENGTH_INTERNAL = 4000;
    private static final String TRUNCATE_INDICATOR = "...";

    private static final String KEY_OPERATION = "operation";
    private static final String KEY_CHANNEL = "channel";
    private static final String KEY_CLIENT_ID = "clientId";
    private static final String KEY_CHAT_ID = "chatId";
    private static final String KEY_COMPANY_ID_MDC = "companyIdMdc";

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void processIncomingMessage(Integer companyId, IncomingChannelMessage incomingMessage) {
        MdcUtil.withContext(
                () -> {
                    log.info("Processing unified incoming message from channel {} for user/ID {} in company {}",
                            incomingMessage.getChannel(), incomingMessage.getChannelSpecificUserId(), companyId);
                    MDC.put(KEY_COMPANY_ID_MDC, String.valueOf(companyId));

                    Client client = clientService.findByNameAndCompanyId(incomingMessage.getChannelSpecificUserId(), companyId)
                            .orElseGet(() -> clientService.createClient(incomingMessage.getChannelSpecificUserId(), companyId, null));
                    MDC.put(KEY_CLIENT_ID, String.valueOf(client.getId()));

                    Optional<Chat> existingOpenChatOpt;
                    if (incomingMessage.getExternalChatId() != null && !incomingMessage.getExternalChatId().isBlank()) {
                        existingOpenChatOpt = chatServiceFacade.findOpenChatByClientAndChannelAndExternalId(
                                client.getId(), incomingMessage.getChannel(), incomingMessage.getExternalChatId()
                        );
                    } else {
                        existingOpenChatOpt = chatServiceFacade.findOpenChatByClientAndChannel(
                                client.getId(), incomingMessage.getChannel()
                        );
                    }

                    String truncatedContent = truncateContentInternal(incomingMessage.getMessageContent());

                    if (existingOpenChatOpt.isPresent()) {
                        Chat chat = existingOpenChatOpt.get();
                        MDC.put(KEY_CHAT_ID, String.valueOf(chat.getId()));

                        handleExistingChatWithMessage(chat, client, truncatedContent, incomingMessage);
                    } else {
                        handleNewChatCreation(companyId, client, truncatedContent, incomingMessage);
                    }
                    return null;
                },
                KEY_OPERATION, "processIncomingMessage",
                KEY_CHANNEL, incomingMessage.getChannel().name(),
                "channelSpecificUserId", incomingMessage.getChannelSpecificUserId()
        );
    }

    private void handleExistingChatWithMessage(Chat chat, Client client, String content, IncomingChannelMessage incomingMessageDetails) {
        SendMessageRequestDTO messageRequest = new SendMessageRequestDTO();
        messageRequest.setChatId(chat.getId());
        messageRequest.setContent(content);
        messageRequest.setSenderId(client.getId());
        messageRequest.setSenderType(ChatMessageSenderType.CLIENT);
        messageRequest.setExternalMessageId(null);
        messageRequest.setReplyToExternalMessageId(incomingMessageDetails.getReplyToExternalMessageId());

        MessageDto messageDto = chatMessageServiceFacade.processAndSaveMessage(
                messageRequest,
                messageRequest.getSenderId(),
                messageRequest.getSenderType()
        );

        if (containsOperatorRequest(content)) {
            assignOperatorToChat(chat);
        } else {
            Chat potentiallyUpdatedChat = chatRepository.findById(chat.getId()).orElse(chat);
            processAutoResponder(potentiallyUpdatedChat, messageDto);
        }
    }

    private void handleNewChatCreation(Integer companyId, Client client, String content, IncomingChannelMessage incomingMessageDetails) {
        CreateChatRequestDTO createChatRequest = new CreateChatRequestDTO();
        createChatRequest.setClientId(client.getId());
        createChatRequest.setCompanyId(companyId);
        createChatRequest.setChatChannel(incomingMessageDetails.getChannel());
        createChatRequest.setInitialMessageContent(content);
        createChatRequest.setExternalChatId(incomingMessageDetails.getExternalChatId());

        try {
            ChatDetailsDTO createdChatDetails = chatServiceFacade.createChat(createChatRequest);
            MDC.put(KEY_CHAT_ID, String.valueOf(createdChatDetails.getId()));

        } catch (Exception e) {
            log.error("Failed to create {} chat for client ID {}: {}",
                    incomingMessageDetails.getChannel(), client.getId(), e.getMessage(), e);
            chatMetricsService.incrementChatOperationError(
                    "IncomingMessageProcessor.handleNewChatCreation." + incomingMessageDetails.getChannel(),
                    String.valueOf(companyId),
                    e.getClass().getSimpleName()
            );
        }
    }

    private String truncateContentInternal(String content) {
        if (content == null) return null;
        if (content.length() <= MAX_CONTENT_LENGTH_INTERNAL) return content;
        return content.substring(0, MAX_CONTENT_LENGTH_INTERNAL - TRUNCATE_INDICATOR.length()) + TRUNCATE_INDICATOR;
    }

    private void processAutoResponder(Chat chat, MessageDto messageDto) {
        Chat currentChatState = chatRepository.findById(chat.getId()).orElse(chat);

        if (currentChatState.getStatus() == ChatStatus.PENDING_AUTO_RESPONDER) {
            try {
                autoResponderService.processIncomingMessage(messageDto, currentChatState);
            } catch (Exception e) {
                log.error("AutoResponder processing failed for chat {}: {}", currentChatState.getId(), e.getMessage(), e);
                chatMetricsService.incrementChatOperationError(
                        "IncomingMessageProcessor.processAutoResponder",
                        chatMetricHelper.getCompanyIdStr(currentChatState.getCompany()),
                        e.getClass().getSimpleName()
                );
            }
        } else {
            log.debug("Skipping auto-responder for chat {} as its status is {}", currentChatState.getId(), currentChatState.getStatus());
        }
    }

    private boolean containsOperatorRequest(String message) {
        if (message == null || message.isBlank()) return false;
        String lowerCaseText = message.toLowerCase();
        return lowerCaseText.contains("оператор") ||
                lowerCaseText.contains("operator") ||
                lowerCaseText.contains("помощь") ||
                lowerCaseText.contains("help") ||
                lowerCaseText.contains("человек");
    }

    private void assignOperatorToChat(Chat chatInput) {
        Chat chat = chatRepository.findById(chatInput.getId())
                .orElseThrow(() -> new ChatNotFoundException("Chat with ID " + chatInput.getId() + " not found for operator assignment."));

        String companyIdStr = chatMetricHelper.getCompanyIdStr(chat.getCompany());

        try {

            if (chat.getStatus() == ChatStatus.PENDING_AUTO_RESPONDER) {
                autoResponderService.stopForChat(chat.getId());
                log.debug("Stopped AutoResponder for chat ID {} due to operator request.", chat.getId());
            }

            if (chat.getClient() != null) {
                chatServiceFacade.requestOperatorEscalation(chat.getId(), chat.getClient().getId());
            } else {
                log.error("Cannot escalate chat {}: client information is missing.", chat.getId());
                chatMetricsService.incrementChatOperationError(
                        "IncomingMessageProcessor.assignOperatorToChat.escalate",
                        companyIdStr,
                        "MissingClientForEscalation"
                );
                if (chat.getStatus() != ChatStatus.PENDING_OPERATOR) {
                    chat.setStatus(ChatStatus.PENDING_OPERATOR);
                    chat.setUser(null);
                    chat.setAssignedAt(null);
                    chatRepository.save(chat);
                    log.warn("Chat {} set to PENDING_OPERATOR due to operator request but missing client info for full escalation.", chat.getId());
                }
            }
        } catch (Exception e) {
            log.error("Failed to process operator request for chat ID {}: {}", chat.getId(), e.getMessage(), e);
            chatMetricsService.incrementChatOperationError(
                    "IncomingMessageProcessor.assignOperatorToChat",
                    companyIdStr,
                    e.getClass().getSimpleName()
            );
            if (chat.getStatus() != ChatStatus.PENDING_OPERATOR &&
                    chat.getStatus() != ChatStatus.ASSIGNED &&
                    chat.getStatus() != ChatStatus.IN_PROGRESS) {
                chat.setStatus(ChatStatus.PENDING_OPERATOR);
                chat.setUser(null);
                chat.setAssignedAt(null);
                chatRepository.save(chat);
            }
        }
    }
}
