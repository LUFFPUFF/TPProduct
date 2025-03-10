package com.example.domain.api.chat_service_api.service.web_scoket;

import com.example.domain.api.chat_service_api.service.ChatAttachmentService;
import com.example.domain.api.chat_service_api.service.ChatMessageService;
import com.example.domain.api.chat_service_api.service.ChatService;
import com.example.domain.dto.chat_module.ChatAttachmentDto;
import com.example.domain.dto.chat_module.ChatDto;
import com.example.domain.dto.chat_module.MessageDto;
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
            System.out.println("Error while sending message to chat " + chatId + ": " + e.getMessage());
        }
    }

    /**
     * Отправка личного уведомления.
     * @param userId идентификатор пользователя
     * @param notification текст уведомления
     */

    public void sendPersonalNotification(Integer userId, String notification) {
        messagingTemplate.convertAndSendToUser(userId.toString(), "/queue/notifications", notification);
    }

    /**
     * Отправка статуса "Печатает..." в чат.
     * @param chatId идентификатор чата
     * @param typingUserId идентификатор пользователя, который печатает
     */

    public void sendTypingStatus(Integer chatId, Integer typingUserId) {
        messagingTemplate.convertAndSend("/topic/chats/" + chatId + "/typing", typingUserId);
    }

    /**
     * Отправка истории сообщений при подключении к чату.
     * @param chatId идентификатор чата
     */

    public void sendChatHistory(Integer chatId) {
        CompletableFuture.supplyAsync(() -> chatMessageService.getAllMessages())
                .thenAcceptAsync(messages -> messagingTemplate.convertAndSend("/topic/chats/" + chatId, messages));
    }

    /**
     * Отправка статуса "Прочитано" для сообщений.
     * @param chatId идентификатор чата
     * @param messageId идентификатор сообщения
     */

    public void sendReadStatus(Integer chatId, Integer messageId) {
        messagingTemplate.convertAndSend("/topic/chats/" + chatId + "/read", messageId);
    }

    /**
     * Отправка вложения в чат.
     * @param chatId идентификатор чата
     * @param attachmentDto DTO вложения
     */

    public void sendAttachment(Integer chatId, ChatAttachmentDto attachmentDto) {
        chatAttachmentService.createAttachment(attachmentDto);
        messagingTemplate.convertAndSend("/topic/chats/" + chatId + "/attachments", attachmentDto);
    }

    /**
     * Закрытие чата.
     * @param chatId идентификатор чата
     */

    public void closeChat(Integer chatId) {
        ChatDto chatDto = chatService.getChatById(chatId);
        chatDto.setStatus("CLOSED");
        chatService.updateChat(chatId, chatDto);

        messagingTemplate.convertAndSend("/topic/chats/" + chatId + "/status", "CLOSED");
    }

}
