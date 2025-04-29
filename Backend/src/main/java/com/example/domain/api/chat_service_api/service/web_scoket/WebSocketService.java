package com.example.domain.api.chat_service_api.service.web_scoket;

import com.example.domain.api.chat_service_api.exception_handler.exception.service.WebSocketServiceException;
import com.example.domain.api.chat_service_api.service.ChatAttachmentService;
import com.example.domain.api.chat_service_api.service.ChatMessageService;
import com.example.domain.api.chat_service_api.service.ChatService;
import com.example.domain.dto.ChatAttachmentDto;
import com.example.domain.dto.ChatDto;
import com.example.domain.dto.MessageDto;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.transaction.annotation.Propagation;

import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Transactional
public class WebSocketService {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatService chatService;
    private final ChatMessageService chatMessageService;
    private final ChatAttachmentService chatAttachmentService;

    /**
     * Отправка нового сообщения в чат.
     * @param chatId идентификатор чата
     * @param messageDto DTO сообщения
     */
    public void sendMessageToChat(Integer chatId, MessageDto messageDto) {
        try {
            messagingTemplate.convertAndSend("/topic/chats/" + chatId, messageDto);
        } catch (Exception e) {
            throw new WebSocketServiceException("Error while sending message to chat " + chatId + ": " + e.getMessage(), e);
        }
    }

    /**
     * Отправка личного уведомления.
     * @param userId идентификатор пользователя
     * @param notification текст уведомления
     */
    public void sendPersonalNotification(Integer userId, String notification) {
        try {
            messagingTemplate.convertAndSendToUser(userId.toString(), "/queue/notifications", notification);
        } catch (Exception e) {
            throw new WebSocketServiceException("Error while sending personal notification to user " + userId + ": " + e.getMessage(), e);
        }
    }

    /**
     * Отправка статуса "Печатает..." в чат.
     * @param chatId идентификатор чата
     * @param typingUserId идентификатор пользователя, который печатает
     */
    public void sendTypingStatus(Integer chatId, Integer typingUserId) {
        try {
            messagingTemplate.convertAndSend("/topic/chats/" + chatId + "/typing", typingUserId);
        } catch (Exception e) {
            throw new WebSocketServiceException("Error while sending typing status to chat " + chatId + ": " + e.getMessage(), e);
        }
    }

    /**
     * Отправка истории сообщений при подключении к чату.
     * @param chatId идентификатор чата
     */
    public void sendChatHistory(Integer chatId) {
        try {
            CompletableFuture.supplyAsync(chatMessageService::getAllMessages)
                    .thenAcceptAsync(messages -> messagingTemplate.convertAndSend("/topic/chats/" + chatId, messages))
                    .exceptionally(ex -> {
                        throw new WebSocketServiceException("Error while sending chat history for chat " + chatId + ": " + ex.getMessage(), ex);
                    });
        } catch (Exception e) {
            throw new WebSocketServiceException("Error while sending chat history for chat " + chatId + ": " + e.getMessage(), e);
        }
    }

    /**
     * Отправка статуса "Прочитано" для сообщений.
     * @param chatId идентификатор чата
     * @param messageId идентификатор сообщения
     */
    public void sendReadStatus(Integer chatId, Integer messageId) {
        try {
            messagingTemplate.convertAndSend("/topic/chats/" + chatId + "/read", messageId);
        } catch (Exception e) {
            throw new WebSocketServiceException("Error while sending read status for message " + messageId + " in chat " + chatId + ": " + e.getMessage(), e);
        }
    }

    /**
     * Отправка вложения в чат.
     * @param chatId идентификатор чата
     * @param attachmentDto DTO вложения
     */
    public void sendAttachment(Integer chatId, ChatAttachmentDto attachmentDto) {
        try {
            chatAttachmentService.createAttachment(attachmentDto);
            messagingTemplate.convertAndSend("/topic/chats/" + chatId + "/attachments", attachmentDto);
        } catch (Exception e) {
            throw new WebSocketServiceException("Error while sending attachment to chat " + chatId + ": " + e.getMessage(), e);
        }
    }

    /**
     * Закрытие чата.
     * @param chatId идентификатор чата
     */
    public void closeChat(Integer chatId) {
        try {
            ChatDto chatDto = chatService.getChatById(chatId);
            chatDto.setStatus("CLOSED");
            chatService.updateChat(chatId, chatDto);

            messagingTemplate.convertAndSend("/topic/chats/" + chatId + "/status", "CLOSED");
        } catch (Exception e) {
            throw new WebSocketServiceException("Error while closing chat " + chatId + ": " + e.getMessage(), e);
        }
    }
}
