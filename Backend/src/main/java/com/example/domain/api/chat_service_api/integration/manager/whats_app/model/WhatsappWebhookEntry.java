package com.example.domain.api.chat_service_api.integration.manager.whats_app.model;

import lombok.Data;

import java.util.List;

@Data
public class WhatsappWebhookEntry {

    private String id;
    private Long time;
    private List<WhatsappWebhookChange> changes;
}
