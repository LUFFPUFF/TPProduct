package com.example.domain.api.chat_service_api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WebSocketMessagingService {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Отправить сообщение в публичный топик (publish-subscribe).
     * @param destination Топик (например, "/topic/chat/{chatId}/messages").
     * @param payload Объект, который будет преобразован в JSON.
     */
    public void sendMessage(String destination, Object payload) {
        messagingTemplate.convertAndSend(destination, payload);
    }

    /**
     * Отправить сообщение конкретному пользователю (point-to-point).
     * Использует User Destination Prefix ("/user").
     * @param userId ID пользователя (Principal name).
     * @param destination Очередь пользователя (например, "/queue/notifications").
     * @param payload Объект, который будет преобразован в JSON.
     */
    public void sendToUser(String userId, String destination, Object payload) {
        messagingTemplate.convertAndSendToUser(userId, destination, payload);
    }
}
