package com.example.domain.api.chat_service_api.listener;

import com.example.database.model.chats_messages_module.chat.ChatStatus;
import com.example.domain.api.chat_service_api.event.chat.*;
import com.example.domain.api.chat_service_api.event.message.ChatMessageSentEvent;
import com.example.domain.api.chat_service_api.event.message.ChatMessageStatusUpdatedEvent;
import com.example.domain.api.chat_service_api.model.dto.MessageDto;
import com.example.domain.api.chat_service_api.service.WebSocketMessagingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatWebSocketListener {

    private final WebSocketMessagingService messagingService;

    /**
     * Обрабатывает событие отправки нового сообщения в чат.
     * Рассылает сообщение всем участникам чата через WebSocket.
     *
     * @param event Событие отправки сообщения в чат
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleChatMessageSentEvent(ChatMessageSentEvent event) {
        MessageDto messageDto = event.messageDto();
        Integer chatId = messageDto.getChatDto().getId();
        log.info("Handling ChatMessageSentEvent for message ID {} in chat {} via WebSocket.", messageDto.getId(), chatId);
        messagingService.sendChatMessage(chatId, messageDto);
    }

    /**
     * Обрабатывает событие обновления статуса сообщения.
     * Рассылает обновление статуса всем участникам чата через WebSocket.
     *
     * @param event Событие обновления статуса сообщения
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleChatMessageStatusUpdatedEvent(ChatMessageStatusUpdatedEvent event) {
        MessageDto updatedMessageDto = event.updatedMessageDto();
        Integer chatId = updatedMessageDto.getChatDto().getId();
        log.info("Handling ChatMessageStatusUpdatedEvent for message ID {} in chat {} to status {} via WebSocket.",
                updatedMessageDto.getId(), chatId, updatedMessageDto.getStatus());
        messagingService.sendChatMessageStatusUpdate(chatId, updatedMessageDto);
    }

    /**
     * Обрабатывает событие создания нового ожидающего чата.
     * Рассылает уведомление всем подписчикам компании через WebSocket.
     *
     * @param event Событие создания нового ожидающего чата
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleNewPendingChatEvent(NewPendingChatEvent event) {
        log.info("Handling NewPendingChatEvent for chat ID {} (company {}) via WebSocket.", event.chatDto().getId(), event.companyId());
        messagingService.broadcastNewPendingChatToCompany(event.companyId(), event.chatDto());
        messagingService.sendChatStatusUpdate(event.chatDto().getId(), event.chatDto());
    }

    /**
     * Обрабатывает событие назначения оператора на чат.
     * Уведомляет назначенного оператора и рассылает информацию по компании.
     *
     * @param event Событие назначения оператора на чат
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleChatAssignedToOperatorEvent(ChatAssignedToOperatorEvent event) {
        log.info("Handling ChatAssignedToOperatorEvent for chat ID {} to operator {} (company {}) via WebSocket.",
                event.chatDto().getId(), event.operatorPrincipalName(), event.companyId());
        messagingService.notifyOperatorAboutAssignedChat(event.operatorPrincipalName(), event.chatDto());
        messagingService.broadcastAssignedChatToCompany(event.companyId(), event.chatDto());
        messagingService.sendChatStatusUpdate(event.chatDto().getId(), event.chatDto());
    }

    /**
     * Обрабатывает событие привязки оператора к чату.
     * Уведомляет оператора и рассылает информацию по компании.
     *
     * @param event Событие привязки оператора к чату
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOperatorLinkedToChatEvent(OperatorLinkedToChatEvent event) {
        log.info("Handling OperatorLinkedToChatEvent for chat ID {} to operator {} (company {}) via WebSocket.",
                event.chatDto().getId(), event.operatorPrincipalName(), event.companyId());
        messagingService.notifyOperatorAboutAssignedChat(event.operatorPrincipalName(), event.chatDto());
        messagingService.broadcastAssignedChatToCompany(event.companyId(), event.chatDto());
        messagingService.sendChatStatusUpdate(event.chatDto().getId(), event.chatDto());
    }

    /**
     * Обрабатывает событие закрытия чата.
     * Уведомляет оператора (если он был назначен) и рассылает обновление статуса чата.
     *
     * @param event Событие закрытия чата
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleChatClosedEvent(ChatClosedEvent event) {
        log.info("Handling ChatClosedEvent for chat ID {} (company {}) via WebSocket. Operator: {}",
                event.chatDto().getId(), event.companyId(), event.operatorPrincipalName());
        if (event.operatorPrincipalName() != null) {
            messagingService.notifyOperatorAboutClosedChat(event.operatorPrincipalName(), event.chatDto());
        }
        messagingService.sendChatStatusUpdate(event.chatDto().getId(), event.chatDto());
    }

    /**
     * Обрабатывает событие изменения статуса чата.
     * Рассылает обновление статуса всем участникам чата.
     *
     * @param event Событие изменения статуса чата
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void handleChatStatusChangedEvent(ChatStatusChangedEvent event) {
        log.info("Handling ChatStatusChangedEvent for chat ID {} to status {} via WebSocket.",
                event.chatId(), event.chatDtoWithNewStatus().getStatus());
        messagingService.sendChatStatusUpdate(event.chatId(), event.chatDtoWithNewStatus());
    }

    /**
     * Обрабатывает событие эскалации чата оператору.
     * Рассылает уведомление компании, если чат перешел в статус ожидания оператора.
     *
     * @param event Событие эскалации чата оператору
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleChatEscalatedToOperatorEvent(ChatEscalatedToOperatorEvent event) {
        log.info("Handling ChatEscalatedToOperatorEvent for chat ID {} (company {}) via WebSocket.",
                event.chatDto().getId(), event.companyId());
        if (event.chatDto().getOperator() == null && event.chatDto().getStatus() == ChatStatus.PENDING_OPERATOR) {
            messagingService.broadcastNewPendingChatToCompany(event.companyId(), event.chatDto());
        }
        messagingService.sendChatStatusUpdate(event.chatDto().getId(), event.chatDto());
    }

    /**
     * Обрабатывает событие уведомления в чате.
     * Рассылает уведомление всем участникам чата.
     *
     * @param event Событие уведомления в чате
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleChatNotificationEvent(ChatNotificationEvent event) {
        log.info("Handling ChatNotificationEvent for chat ID {} via WebSocket.", event.chatId());
        messagingService.sendChatNotification(event.chatId(), event.notificationPayload());
    }

    /**
     * Обрабатывает событие тайпинга (набора сообщения) в чате.
     * Рассылает информацию о тайпинге всем участникам чата.
     *
     * @param event Событие тайпинга в чате
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleChatTypingEvent(ChatTypingEvent event) {
        log.debug("Handling ChatTypingEvent for chat ID {} from user {} via WebSocket.",
                event.getChatId(), event.getUserId());
        messagingService.sendChatTypingUpdate(event.getChatId(), event.getTypingPayload());
    }

    /**
     * Обрабатывает событие назначения оператора на чат.
     * Уведомляет нового оператора и рассылает обновление статуса чата.
     *
     * @param event Событие назначения оператора
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOperatorAssignedEvent(OperatorAssignedEvent event) {
        log.info("Handling OperatorAssignedEvent for chat ID {} to operator {} via WebSocket.",
                event.chatId(), event.operatorId());
        messagingService.notifyOperatorAboutAssignedChat(event.operatorId(), event.chatDto());
        messagingService.sendChatStatusUpdate(event.chatId(), event.chatDto());
    }
}
