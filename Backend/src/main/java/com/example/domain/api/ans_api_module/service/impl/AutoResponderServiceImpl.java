package com.example.domain.api.ans_api_module.service.impl;

import com.example.database.model.chats_messages_module.chat.Chat;
import com.example.database.model.chats_messages_module.chat.ChatMessageSenderType;
import com.example.database.model.chats_messages_module.chat.ChatStatus;
import com.example.database.model.chats_messages_module.message.ChatMessage;
import com.example.domain.api.ans_api_module.answer_finder.dto.AnswerSearchResultItem;
import com.example.domain.api.ans_api_module.answer_finder.exception.AnswerSearchException;
import com.example.domain.api.ans_api_module.answer_finder.service.AnswerSearchService;
import com.example.domain.api.ans_api_module.correction_answer.dto.GenerationRequest;
import com.example.domain.api.ans_api_module.correction_answer.exception.MLException;
import com.example.domain.api.ans_api_module.correction_answer.service.GenerationType;
import com.example.domain.api.ans_api_module.correction_answer.service.TextProcessingApiClient;
import com.example.domain.api.ans_api_module.correction_answer.service.TextProcessingService;
import com.example.domain.api.ans_api_module.event.AutoResponderEscalationEvent;
import com.example.domain.api.ans_api_module.exception.AutoResponderException;
import com.example.domain.api.ans_api_module.service.IAutoResponderService;
import com.example.domain.api.chat_service_api.exception_handler.exception.ExternalMessagingException;
import com.example.domain.api.chat_service_api.integration.service.IExternalMessagingService;
import com.example.domain.api.chat_service_api.mapper.ChatMessageMapper;
import com.example.domain.api.chat_service_api.model.dto.MessageDto;
import com.example.domain.api.chat_service_api.model.rest.mesage.SendMessageRequestDTO;
import com.example.domain.api.chat_service_api.service.IChatMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AutoResponderServiceImpl implements IAutoResponderService {

    private final AnswerSearchService answerSearchService;
    private final TextProcessingService textProcessingService;
    private final IChatMessageService chatMessageService;
    private final ChatMessageMapper messageMapper;
    private final IExternalMessagingService externalMessagingService;
    private final ApplicationEventPublisher eventPublisher;

    private static final ChatMessageSenderType AUTO_RESPONDER_SENDER_TYPE = ChatMessageSenderType.AUTO_RESPONDER;
    private static final double RELEVANCE_THRESHOLD = 0.6;

    @Override
    public void processNewPendingChat(Integer chatId) throws AutoResponderException {
        Optional<Chat> chatOptional = chatMessageService.findChatEntityById(chatId);
        if (chatOptional.isEmpty()) {
            log.warn("AutoResponder: Chat ID {} not found for processing.", chatId);
            throw new AutoResponderException("AutoResponder: Chat ID " + chatId + " not found.");
        }

        Chat chat = chatOptional.get();

        if (chat.getStatus() != ChatStatus.PENDING_AUTO_RESPONDER) {
            log.warn("AutoResponder: Chat ID {} is not in PENDING_AUTO_RESPONDER status (current status: {}). Skipping.", chatId, chat.getStatus());
            return;
        }

        try {
            Optional<ChatMessage> firstClientMessageOptional = chatMessageService.findFirstMessageByChatId(chatId);
            if (firstClientMessageOptional.isPresent()) {
                ChatMessage firstMessage = firstClientMessageOptional.get();
                MessageDto firstMessageDTO = messageMapper.toDto(firstMessage);

                processIncomingMessage(firstMessageDTO, chat);
            } else {
                log.warn("AutoResponder: No first message found in new pending chat ID {}", chatId);
            }
        } catch (Exception e) {
            log.error("Error during initial auto-responder processing for chat {}", chatId, e);
            if (chat.getStatus() == ChatStatus.PENDING_AUTO_RESPONDER) {
                log.warn("AutoResponder: Publishing escalation event due to initial processing error for chat ID {}...", chatId);
                eventPublisher.publishEvent(new AutoResponderEscalationEvent(this, chat.getId(), chat.getClient().getId()));
                sendAutoResponderMessage(chat, "Извините, произошла ошибка при первом обращении. Передаю ваш вопрос оператору.");
                log.warn("AutoResponder: Escalation event published for chat ID {} due to initial error.", chatId);
            }
            throw new AutoResponderException("Error during initial auto-responder processing for chat " + chatId, e);
        }

    }

    public void processIncomingMessage(MessageDto messageDTO, Chat chat) throws AutoResponderException {
        log.info("AutoResponder: Processing incoming message ID {} for chat ID {}",
                messageDTO.getId(), messageDTO.getChatDto().getId());

        if (chat.getStatus() != ChatStatus.PENDING_AUTO_RESPONDER || messageDTO.getSenderType() != ChatMessageSenderType.CLIENT) {
            log.debug("AutoResponder: Skipping message ID {} in chat ID {}. Status: {}, SenderType: {}",
                    messageDTO.getId(), chat.getId(), chat.getStatus(), messageDTO.getSenderType());
            return;
        }

        String clientQuery = messageDTO.getContent();
        Integer companyId = chat.getCompany() != null ? chat.getCompany().getId() : null;
        String companyDescription = (chat.getCompany() != null && chat.getCompany().getCompanyDescription() != null)
                ? chat.getCompany().getCompanyDescription()
                : null;


        if (clientQuery == null || clientQuery.trim().isEmpty()) {
            log.warn("AutoResponder: Received empty client query in message ID {}. Skipping processing.", messageDTO.getId());
            return;
        }

        String correctedQuery = "";
        List<AnswerSearchResultItem> relevantAnswers = Collections.emptyList();

        //TODO временное решение по отключению коррекции
//        try {
//            correctedQuery = textProcessingService.processQuery(clientQuery, GenerationType.CORRECTION);
//            log.debug("AutoResponder: Corrected client query for message ID {}: {}", messageDTO.getId(), correctedQuery);
//        } catch (MLException e) {
//            log.warn("AutoResponder: Failed to correct client query for message ID {} due to TextProcessingService error. Using original query. Error: {}",
//                    messageDTO.getId(), e.getMessage(), e);
//            correctedQuery = clientQuery;
//        } catch (Exception e) {
//            log.error("AutoResponder: Unexpected error correcting client query for message ID {}. Using original query. Error: {}",
//                    messageDTO.getId(), e.getMessage(), e);
//            correctedQuery = clientQuery;
//        }

        boolean answerFound = false;
        String autoResponderResponse = null;

        try {
            relevantAnswers = answerSearchService.findRelevantAnswers(
                    correctedQuery,
                    companyId,
                    null
            );

            log.debug("AutoResponder: Found {} relevant answers for chat ID {}. Query: {}",
                    relevantAnswers.size(), chat.getId(), correctedQuery);

            Optional<AnswerSearchResultItem> bestAnswer = relevantAnswers.stream()
                    .findFirst();

            if (bestAnswer.isPresent() && bestAnswer.get().getScore() >= RELEVANCE_THRESHOLD) {
                AnswerSearchResultItem answer = bestAnswer.get();
                String originalAnswerText = answer.getAnswer().getAnswer();
                log.info("AutoResponder: Found relevant predefined answer (score {}) for chat ID {}.",
                        answer.getScore(), chat.getId());

                try {
                    autoResponderResponse = textProcessingService.processQuery(originalAnswerText, GenerationType.REWRITE);
                    log.debug("AutoResponder: Rewritten predefined answer for chat ID {}: {}", chat.getId(), autoResponderResponse);
                } catch (MLException e) {
                    log.warn("AutoResponder: Failed to rewrite predefined answer for chat ID {} due to TextProcessingService error. Using original answer. Error: {}",
                            chat.getId(), e.getMessage(), e);
                    autoResponderResponse = originalAnswerText;
                } catch (Exception e) {
                    log.error("AutoResponder: Unexpected error rewriting predefined answer for chat ID {}. Using original answer. Error: {}",
                            chat.getId(), e.getMessage(), e);
                    autoResponderResponse = originalAnswerText;
                }

                answerFound = true;

            } else {
                log.info("AutoResponder: No relevant predefined answer found (or score < {}) for chat ID {}. Attempting general generation.",
                        RELEVANCE_THRESHOLD, chat.getId());

                if (companyDescription != null && !companyDescription.trim().isEmpty()) {
                    try {

                        autoResponderResponse = textProcessingService.generateGeneralAnswer(correctedQuery, companyDescription);

                        if (autoResponderResponse != null && !autoResponderResponse.trim().isEmpty()) {
                            log.info("AutoResponder: Successfully generated general answer for chat ID {}.", chat.getId());
                            answerFound = true;
                        } else {
                            log.warn("AutoResponder: General answer generation returned empty/null response for chat ID {}.", chat.getId());
                        }

                    } catch (MLException e) {
                        log.warn("AutoResponder: Failed to generate general answer for chat ID {} due to TextProcessingService error. Error: {}",
                                chat.getId(), e.getMessage(), e);
                    } catch (Exception e) {
                        log.error("AutoResponder: Unexpected error generating general answer for chat ID {}. Error: {}",
                                chat.getId(), e.getMessage(), e);
                    }
                } else {
                    log.warn("AutoResponder: Company description is missing or empty for chat ID {}. Cannot generate general answer.", chat.getId());
                }
            }

            if (answerFound && autoResponderResponse != null && !autoResponderResponse.trim().isEmpty()) {
                sendAutoResponderMessage(chat, autoResponderResponse);
            } else {
                log.info("AutoResponder: Could not find or generate a suitable answer for chat ID {}. Publishing escalation event.", chat.getId());
                if (chat.getStatus() == ChatStatus.PENDING_AUTO_RESPONDER) {
                    eventPublisher.publishEvent(new AutoResponderEscalationEvent(this, chat.getId(), chat.getClient().getId()));
                    sendAutoResponderMessage(chat, "Извините, я не смог найти ответ на ваш вопрос и сгенерировать подходящий ответ. Передаю ваш вопрос оператору.");
                    log.info("AutoResponder: Escalation event published for chat ID {}.", chat.getId());
                } else {
                    log.debug("AutoResponder: Chat ID {} is already escalated or handled (status {}). Not re-escalating.", chat.getId(), chat.getStatus());
                }
            }

        } catch (AnswerSearchException e) {
            log.error("AutoResponder: Error during answer search for message ID {} in chat ID {}: {}",
                    messageDTO.getId(), chat.getId(), e.getMessage(), e);

            if (chat.getStatus() == ChatStatus.PENDING_AUTO_RESPONDER) {
                log.warn("AutoResponder: Publishing escalation event due to answer search error for chat ID {}...", chat.getId());
                eventPublisher.publishEvent(new AutoResponderEscalationEvent(this, chat.getId(), chat.getClient().getId()));
                sendAutoResponderMessage(chat, "Извините, возникла проблема при поиске ответа. Передаю ваш вопрос оператору.");
                log.warn("AutoResponder: Escalation event published for chat ID {} due to search error.", chat.getId());
            }
            throw new AutoResponderException("Error in auto-responder answer search for chat " + chat.getId(), e);

        } catch (Exception e) {
            log.error("AutoResponder: Unexpected error processing message ID {} for chat ID {}: {}",
                    messageDTO.getId(), chat.getId(), e.getMessage(), e);
            if (chat.getStatus() == ChatStatus.PENDING_AUTO_RESPONDER) {
                log.warn("AutoResponder: Publishing escalation event due to unexpected error for chat ID {}...", chat.getId());
                eventPublisher.publishEvent(new AutoResponderEscalationEvent(this, chat.getId(), chat.getClient().getId()));
                sendAutoResponderMessage(chat, "Извините, произошла непредвиденная ошибка. Передаю ваш вопрос оператору.");
                log.error("AutoResponder: Escalation event published for chat ID {} due to unexpected error.", chat.getId());
            }
            throw new AutoResponderException("Unexpected error in auto-responder processing for chat " + chat.getId(), e);
        }

        log.info("AutoResponder: Finished message processing for message ID {} in chat ID {}",
                messageDTO.getId(), chat.getId());
    }

    @Override
    public void stopForChat(Integer chatId) {
        //TODO какая должна быть логика остановки автоответчика?
    }

    @Transactional
    protected void sendAutoResponderMessage(Chat chat, String content) {
        SendMessageRequestDTO messageRequest = new SendMessageRequestDTO();
        messageRequest.setChatId(chat.getId());
        messageRequest.setContent(content);
        messageRequest.setExternalMessageId(null);
        messageRequest.setReplyToExternalMessageId(null);

        Integer AUTO_RESPONDER_INTERNAL_USER_ID = chat.getClient().getId();

        messageRequest.setSenderId(AUTO_RESPONDER_INTERNAL_USER_ID);
        messageRequest.setSenderType(AUTO_RESPONDER_SENDER_TYPE);

        try {
            chatMessageService.processAndSaveMessage(
                    messageRequest,
                    messageRequest.getSenderId(),
                    messageRequest.getSenderType()
            );
            log.info("AutoResponder: System message saved internally for chat ID {}.", chat.getId());
        } catch (Exception e) {
            log.error("AutoResponder: Failed to save system message internally for chat {}: {}", chat.getId(), e.getMessage(), e);
            //TODO реализация повтора попытки и если не полчучилось - перевод на оператора
        }

        if (content != null && !content.trim().isEmpty()) {
            try {
                externalMessagingService.sendMessageToExternal(chat.getId(), content);
                log.info("AutoResponder: Message successfully sent to external channel for chat ID {}.", chat.getId());
            } catch (ExternalMessagingException e) {
                log.error("AutoResponder: Failed to send message to external channel for chat {}: {}", chat.getId(), e.getMessage(), e);
                // TODO: Что делать при ошибке внешней отправки? Залогировать, уведомить оператора?
            }
        }
    }
}
