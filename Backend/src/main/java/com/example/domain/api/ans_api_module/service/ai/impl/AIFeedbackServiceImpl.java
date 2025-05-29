package com.example.domain.api.ans_api_module.service.ai.impl;

import com.example.database.model.ai_module.AIFeedback;
import com.example.database.model.ai_module.AIResponses;
import com.example.database.model.chats_messages_module.chat.Chat;
import com.example.database.model.chats_messages_module.chat.ChatMessageSenderType;
import com.example.database.model.chats_messages_module.message.ChatMessage;
import com.example.database.model.crm_module.client.Client;
import com.example.database.repository.ai_module.AIFeedbackRepository;
import com.example.database.repository.ai_module.AIResponsesRepository;
import com.example.database.repository.chats_messages_module.ChatRepository;
import com.example.database.repository.crm_module.ClientRepository;
import com.example.domain.api.ans_api_module.service.ai.IAIFeedbackService;
import com.example.domain.api.chat_service_api.exception_handler.ChatNotFoundException;
import com.example.domain.api.chat_service_api.exception_handler.ResourceNotFoundException;
import com.example.domain.api.chat_service_api.exception_handler.exception.ExternalMessagingException;
import com.example.domain.api.chat_service_api.integration.service.IExternalMessagingService;
import com.example.domain.api.chat_service_api.model.dto.MessageDto;
import com.example.domain.api.chat_service_api.model.rest.mesage.SendMessageRequestDTO;
import com.example.domain.api.chat_service_api.service.IChatMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AIFeedbackServiceImpl implements IAIFeedbackService {

    private final AIResponsesRepository aiResponsesRepository;
    private final AIFeedbackRepository aiFeedbackRepository;
    private final ChatRepository chatRepository;
    private final IChatMessageService chatMessageServiceFacade;
    private final ClientRepository clientRepository;
    private final IExternalMessagingService externalMessagingService;

    @Override
    @Transactional
    public AIResponses logAiResponse(Chat chat, Client client, ChatMessage aiChatMessage, Float confidence) {
        if (aiChatMessage == null || chat == null || client == null) {
            log.error("Cannot log AI response due to null input: chat={}, client={}, aiChatMessage={}",
                    chat != null ? chat.getId() : "null",
                    client != null ? client.getId() : "null",
                    aiChatMessage != null ? aiChatMessage.getId() : "null");

            if (aiChatMessage == null || aiChatMessage.getId() == null) {
                throw new IllegalArgumentException("Cannot log AI response without a valid persisted ChatMessage.");
            }
        }
        AIResponses aiResponse = new AIResponses();
        aiResponse.setChat(chat);
        aiResponse.setClient(client);
        aiResponse.setChatMessage(aiChatMessage);
        aiResponse.setResponsesText(aiChatMessage.getContent());
        aiResponse.setConfidence(confidence);
        aiResponse.setCreatedAt(LocalDateTime.now());
        return aiResponsesRepository.save(aiResponse);
    }

    @Override
    public void requestFeedbackFromClient(Chat chatInput, Integer lastAiResponseId) {
        if (chatInput == null || chatInput.getClient() == null) {
            log.error("Cannot request feedback for chat {} - chat or client is null.", chatInput != null ? chatInput.getId() : "null");
            return;
        }

        Chat chat = chatRepository.findById(chatInput.getId())
                .orElseThrow(() -> new ChatNotFoundException("Chat not found for requesting feedback: " + chatInput.getId()));

        log.info("Requesting feedback from client {} in chat {}", chat.getClient().getId(), chat.getId());

        String lastResponseContext = "";
        if (lastAiResponseId != null) {
            Optional<AIResponses> lastAiResp = aiResponsesRepository.findById(lastAiResponseId);
            if (lastAiResp.isPresent() && StringUtils.hasText(lastAiResp.get().getResponsesText())) {
                String respText = lastAiResp.get().getResponsesText();
                lastResponseContext = " (касательно моего предыдущего ответа: \"" +
                        respText.substring(0, Math.min(respText.length(), 50)) +
                        (respText.length() > 50 ? "..." : "") +
                        "\")";
            }
        }

        SendMessageRequestDTO messageRequest = getSendMessageRequestDTO(chat, lastResponseContext);

        try {
            MessageDto savedMessageDto = chatMessageServiceFacade.processAndSaveMessage(
                    messageRequest,
                    messageRequest.getSenderId(),
                    messageRequest.getSenderType()
            );

            if (lastAiResponseId != null) {
                chat.setAwaitingFeedbackForAiResponseId(lastAiResponseId);
                chatRepository.save(chat);
                log.info("Chat {} marked as awaiting feedback for AI Response ID {}", chat.getId(), lastAiResponseId);
            } else {
                log.warn("lastAiResponseId is null for chat {}, cannot mark for specific feedback.", chat.getId());
            }

            if (savedMessageDto != null && StringUtils.hasText(savedMessageDto.getContent())) {
                try {
                    externalMessagingService.sendMessageToExternal(chat.getId(), savedMessageDto.getContent());
                    log.info("Feedback request message (ID {}) successfully placed for external sending for chat ID {}.",
                            savedMessageDto.getId(), chat.getId());
                } catch (ExternalMessagingException e) {
                    log.error("Failed to send feedback request message (ID {}) to external channel for chat {}: {}",
                            savedMessageDto.getId(), chat.getId(), e.getMessage(), e);
                } catch (Exception e) {
                    log.error("Unexpected error sending feedback request message (ID {}) to external channel for chat {}: {}",
                            savedMessageDto.getId(), chat.getId(), e.getMessage(), e);
                }
            } else {
                log.warn("Feedback request message was saved but DTO or content is null/empty for chat {}. Not sending to external.", chat.getId());
            }

            log.info("Feedback request message sent to client in chat {}", chat.getId());
        } catch (Exception e) {
            log.error("Failed to send feedback request message to client in chat {}: {}", chat.getId(), e.getMessage(), e);
        }
    }

    private static @NotNull SendMessageRequestDTO getSendMessageRequestDTO(Chat chat, String lastResponseContext) {
        String feedbackRequestMessage = String.format(
                "Мы переключаем вас на оператора. Прежде чем мы продолжим, пожалуйста, оцените качество моих ответов%s по шкале от 1 до 5 (где 5 - отлично). " +
                        "Вы также можете добавить комментарий. Например: \"Оценка: 4. Комментарий: Ответ был полезен.\"",
                lastResponseContext
        );

        SendMessageRequestDTO messageRequest = new SendMessageRequestDTO();
        messageRequest.setChatId(chat.getId());
        messageRequest.setContent(feedbackRequestMessage);
        messageRequest.setSenderId(chat.getClient().getId());
        messageRequest.setSenderType(ChatMessageSenderType.AUTO_RESPONDER);
        return messageRequest;
    }

    @Override
    @Transactional
    public void saveClientFeedback(Integer aiResponseId, Integer clientId, double rating, String comment) {
        if (aiResponseId == null || clientId == null) {
            log.error("Cannot save client feedback: aiResponseId or clientId is null.");
            return;
        }

        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> {
                    log.error("Client not found for feedback: {}", clientId);
                    return new ResourceNotFoundException("Client not found: " + clientId);
                });

        AIResponses aiResponse = aiResponsesRepository.findById(aiResponseId)
                .orElseThrow(() -> {
                    log.error("AIResponse not found for feedback: {}", aiResponseId);
                    return new ResourceNotFoundException("AI Response not found: " + aiResponseId);
                });

        if (!Objects.equals(aiResponse.getClient().getId(), clientId)) {
            log.warn("Client ID mismatch for feedback: AIResponse client ID is {}, feedback client ID is {}. Feedback for AIResponse ID {} will still be saved.",
                    aiResponse.getClient().getId(), clientId, aiResponseId);
        }

        AIFeedback feedback = new AIFeedback();
        feedback.setResponses(aiResponse);
        feedback.setClient(client);
        feedback.setRating((int) Math.round(rating));
        feedback.setComment(comment != null && comment.length() > 2048 ? comment.substring(0, 2048) : comment);
        feedback.setCreatedAt(LocalDateTime.now());

        aiFeedbackRepository.save(feedback);
        log.info("Saved client feedback for AI response ID {}, client ID {}, rating {}", aiResponseId, clientId, rating);
    }

    @Override
    public Optional<Integer> findLastLoggedAiResponseIdForChat(Integer chatId) {
        if (chatId == null) {
            log.warn("findLastLoggedAiResponseIdForChat called with null chatId.");
            return Optional.empty();
        }

        return aiResponsesRepository.findTopByChatIdOrderByCreatedAtDesc(chatId)
                .map(AIResponses::getId);
    }
}
