package com.example.domain.api.chat_service_api.integration.manager.whats_app.model;

import lombok.Data;

@Data
public class WhatsappWebhookChange {

    private String field;
    private WhatsappWebhookValue value;
}
