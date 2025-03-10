package com.example.domain.api.chat_service_api.config.chat;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
@Getter @Setter
public class ChatConfig {

    @Value("${chat.limits.max-messages-per-chat}")
    private int maxMessagesPerChat;

    @Value("${chat.limits.max-attachments-per-message}")
    private int maxAttachmentsPerMessage;

    @Getter
    @Value("${chat.lifetime.inactive-chat-close-time}")
    private String inactiveChatCloseTime;

    @Value("${chat.status.active}")
    private String activeStatus;

    @Value("${chat.status.closed}")
    private String closedStatus;

    @Value("${chat.status.pending}")
    private String pendingStatus;
}
