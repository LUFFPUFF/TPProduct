package com.example.domain.api.chat_service_api.controller;

import com.example.domain.api.chat_service_api.exception_handler.exception.controller.ChatControllerException;
import com.example.domain.api.chat_service_api.service.ChatMessageService;
import com.example.domain.api.chat_service_api.service.web_scoket.WebSocketService;
import com.example.domain.dto.ChatAttachmentDto;
import com.example.domain.dto.ChatDto;
import com.example.domain.api.chat_service_api.service.ChatService;
import com.example.domain.dto.MessageDto;
import com.example.domain.dto.chat_module.web_socket.NotificationDTO;
import com.example.domain.dto.chat_module.web_socket.ReadStatusDTO;
import com.example.domain.dto.chat_module.web_socket.TypingStatusDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chats")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final ChatMessageService chatMessageService;
    private final WebSocketService webSocketService;

    @GetMapping
    public List<ChatDto> getAllChats() {
        return chatService.getAllChats();
    }

    @GetMapping("/{id}")
    public ChatDto getChatById(@PathVariable Integer id) {
        return chatService.getChatById(id);
    }

    @PostMapping
    public ChatDto createChat(@RequestBody ChatDto chatDto) {
        return chatService.createChat(chatDto);
    }

    @PutMapping("/{id}")
    public ChatDto updateChat(@PathVariable Integer id, @RequestBody ChatDto chatDto) {
        return chatService.updateChat(id, chatDto);
    }

    @DeleteMapping("/{id}")
    public void deleteChat(@PathVariable Integer id) {
        chatService.deleteChat(id);
        webSocketService.closeChat(id);
    }

    /**
     * Обработка отправки нового сообщения.
     * @param messageDto DTO сообщения
     */
    @MessageMapping("/sendMessage")
    public void handleSendMessage(@Payload MessageDto messageDto) {
        try {
            System.out.println("Received message: " + messageDto);

            MessageDto savedMessage = chatMessageService.createMessage(messageDto);

            webSocketService.sendMessageToChat(savedMessage.getChatDto().getId(), savedMessage);
        } catch (ChatControllerException e) {
            throw new ChatControllerException("Error while sending message: ", e);
        }
    }

    /**
     * Обработка отправки "Печатает..." в чат.
     * @param typingStatusDTO DTO статуса
     */
    @MessageMapping("${spring.websocket.application-destination-prefix}/typing")
    public void handleTypingStatus(@Payload TypingStatusDTO typingStatusDTO) {
        webSocketService.sendTypingStatus(typingStatusDTO.getChatId(), typingStatusDTO.getTypingUserId());
    }

    /**
     * Обработка получения истории сообщений при подключении.
     * @param chatId идентификатор чата
     */
    @MessageMapping("${spring.websocket.application-destination-prefix}/getChatHistory")
    public void handleGetChatHistory(@Payload Integer chatId) {
        webSocketService.sendChatHistory(chatId);
    }

    /**
     * Обработка отправки уведомления пользователю.
     * @param notificationDTO DTO уведомления
     */
    @MessageMapping("${spring.websocket.application-destination-prefix}/sendNotification")
    public void handleSendNotification(@Payload NotificationDTO notificationDTO) {
        webSocketService.sendPersonalNotification(notificationDTO.getUserId(), notificationDTO.getNotification());
    }

    /**
     * Обработка статуса "Прочитано" для сообщений.
     * @param readStatusDTO DTO статуса "Прочитано"
     */
    @MessageMapping("${spring.websocket.application-destination-prefix}/sendReadStatus")
    public void handleReadStatus(@Payload ReadStatusDTO readStatusDTO) {
        webSocketService.sendReadStatus(readStatusDTO.getChatId(), readStatusDTO.getMessageId());
    }

    /**
     * Обработка отправки вложения.
     * @param attachmentDto DTO вложения
     */
    @MessageMapping("${spring.websocket.application-destination-prefix}/sendAttachment")
    public void handleSendAttachment(@Payload ChatAttachmentDto attachmentDto) {
        MessageDto message = chatMessageService.getMessageById(attachmentDto.getMessageId());

        if (message != null) {
            webSocketService.sendAttachment(message.getChatDto().getId(), attachmentDto);
        } else {
            throw new ChatControllerException("Message not found for ID: " +  attachmentDto.getMessageId());
        }
    }

    /**
     * Обработка закрытия чата.
     * @param chatId идентификатор чата
     */
    @MessageMapping("${spring.websocket.application-destination-prefix}/closeChat")
    public void handleCloseChat(@Payload Integer chatId) {
        webSocketService.closeChat(chatId);
    }
}
