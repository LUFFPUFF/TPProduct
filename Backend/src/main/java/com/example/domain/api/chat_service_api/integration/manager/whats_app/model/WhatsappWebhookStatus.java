package com.example.domain.api.chat_service_api.integration.manager.whats_app.model;

import lombok.Data;

@Data
public class WhatsappWebhookStatus {

    private String id;
    private String status;
    private String from;
    private Long timestamp;
}
