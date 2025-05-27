package com.example.domain.api.chat_service_api.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@ConfigurationProperties(prefix = "websocket.topics")
@Data
@Validated
public class WebSocketTopicRegistry {

    @NotBlank
    @Value("${websocket.topics.company.pending-chats}")
    private String companyPendingChats;
    @NotBlank
    @Value("${websocket.topics.company.assigned-chats}")
    private String companyAssignedChats;
    @NotBlank
    @Value("${websocket.topics.chat.base}")
    private String chatBase;
    @NotBlank
    @Value("${websocket.topics.chat.status}")
    private String chatStatusSuffix;
    @NotBlank
    @Value("${websocket.topics.chat.messages}")
    private String chatMessagesSuffix;
    @NotBlank
    @Value("${websocket.topics.chat.typing}")
    private String chatTypingSuffix;
    @Value("${websocket.topics.chat.notifications}")
    private String chatNotificationsSuffix;
    @NotBlank
    @Value("${websocket.topics.user.base}")
    private String userBase;
    @NotBlank
    @Value("${websocket.topics.user.notifications}")
    private String userNotificationsSuffix;
    @NotBlank
    @Value("${websocket.topics.user.assigned-chats}")
    private String userAssignedChatsSuffix;
    @NotBlank
    @Value("${websocket.topics.user.chat-closed}")
    private String userChatClosedSuffix;

    public String getCompanyPendingChatsTopic(Object companyId) {
        return companyPendingChats.replace("{companyId}", String.valueOf(companyId));
    }

    public String getCompanyAssignedChatsTopic(Object companyId) {
        return companyAssignedChats.replace("{companyId}", String.valueOf(companyId));
    }

    public String getChatStatusTopic(Object chatId) {
        return chatBase.replace("{chatId}", String.valueOf(chatId)) + chatStatusSuffix;
    }

    public String getChatMessagesTopic(Object chatId) {
        return chatBase.replace("{chatId}", String.valueOf(chatId)) + chatMessagesSuffix;
    }

    public String getChatTypingTopic(Object chatId) {
        return chatBase.replace("{chatId}", String.valueOf(chatId)) + chatTypingSuffix;
    }
    public String getChatNotificationsTopic(Object chatId) {
        return chatBase.replace("{chatId}", String.valueOf(chatId)) + chatNotificationsSuffix;
    }
    public String getFullUserNotificationsTopic(Object userId) {
        return userBase.replace("{userId}", String.valueOf(userId)) + userNotificationsSuffix;
    }

    public String getFullUserAssignedChatsTopic(Object userId) {
        return userBase.replace("{userId}", String.valueOf(userId)) + userAssignedChatsSuffix;
    }

    public String getFullUserChatClosedTopic(Object userId) {
        return userBase.replace("{userId}", String.valueOf(userId)) + userChatClosedSuffix;
    }
    public String getUserNotificationsQueueSuffix() {
        return userNotificationsSuffix;
    }
    public String getUserAssignedChatsQueueSuffix() {
        return userAssignedChatsSuffix;
    }

    public String getUserChatClosedQueueSuffix() {
        return userChatClosedSuffix;
    }
}
