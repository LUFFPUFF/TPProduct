package com.example.domain.api.chat_service_api.integration.manager.whats_app.model;

import lombok.Data;

@Data
public class WhatsappWebhookMessage {

    private String id;
    private String from;
    private Long timestamp;
    private String type;

    private WhatsappWebhookTextMessage text;
}
