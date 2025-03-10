package com.example.domain.api.chat_service_api.config.chat;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
public class NotificationConfig {

    @Value("${notification.templates.new-message}")
    private String newMessageTemplate;

    @Value("${notification.templates.operator-typing}")
    private String operatorTypingTemplate;

    @Value("${notification.interval}")
    private String notificationInterval;
}
