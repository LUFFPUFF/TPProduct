package com.example.domain.api.chat_service_api.controller;

import com.example.database.model.chats_messages_module.chat.ChatMessageSenderType;
import com.example.domain.api.chat_service_api.event.chat.ChatTypingEvent;
import com.example.domain.api.chat_service_api.event.payload.TypingPayload;
import com.example.domain.security.model.UserContext;
import com.example.domain.security.util.UserContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;


@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatInteractionController {

    private final ApplicationEventPublisher eventPublisher;

    @MessageMapping("/chat/{chatId}/typing")
    public void handleTyping(@DestinationVariable("chatId") Integer chatId,
                             @Payload TypingPayload requestPayload,
                             SimpMessageHeaderAccessor headerAccessor) {

        UserContext userContext = UserContextHolder.getContext();

        log.debug("Received typing event for chat [{}]. Payload: {}. UserContext present: {}",
                chatId, requestPayload, userContext != null);

        String effectiveUserId;
        String effectiveUserName;
        ChatMessageSenderType effectiveSenderType;

        if (userContext != null) {
            effectiveUserId = String.valueOf(userContext.getUserId());
            effectiveSenderType = ChatMessageSenderType.OPERATOR;

            effectiveUserName = userContext.getEmail();
            if (effectiveUserName == null || effectiveUserName.isBlank()) {
                effectiveUserName = "Оператор " + effectiveUserId;
            }

            if (requestPayload.getSenderType() != null && requestPayload.getSenderType() != ChatMessageSenderType.OPERATOR) {
                log.warn("Authenticated user (from UserContext) {} sent typing event with unexpected senderType {}. Overriding to OPERATOR.",
                        effectiveUserId, requestPayload.getSenderType());
            }
        } else {
            if (requestPayload.getSenderType() != ChatMessageSenderType.CLIENT) {
                log.warn("Unauthenticated user (session: {}) sent typing event for chat {} with non-CLIENT senderType: {}. Assuming CLIENT.",
                        headerAccessor.getSessionId(), chatId, requestPayload.getSenderType());
                effectiveSenderType = ChatMessageSenderType.CLIENT;
            } else {
                effectiveSenderType = requestPayload.getSenderType();
            }

            effectiveUserId = requestPayload.getUserId();
            effectiveUserName = requestPayload.getUserName() != null ? requestPayload.getUserName() : "Клиент";

            if (effectiveUserId == null || effectiveUserId.isBlank()) {
                effectiveUserId = "anon_" + headerAccessor.getSessionId();
                log.warn("Client did not provide userId for typing event in chat {}. Using session-based ID: {}", chatId, effectiveUserId);
            }

        }

        TypingPayload eventPayload = new TypingPayload(
                effectiveUserId,
                effectiveUserName,
                effectiveSenderType,
                requestPayload.isTyping()
        );

        eventPublisher.publishEvent(new ChatTypingEvent(this, chatId, effectiveUserId, eventPayload));
    }
}
