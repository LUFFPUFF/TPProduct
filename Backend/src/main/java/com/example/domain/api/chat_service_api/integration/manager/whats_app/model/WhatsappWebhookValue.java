package com.example.domain.api.chat_service_api.integration.manager.whats_app.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class WhatsappWebhookValue {

    @JsonProperty("messaging_product")
    private String messagingProduct;
    private WhatsappWebhookMetadata metadata;
    private List<WhatsappWebhookMessage> messages;
    private List<WhatsappWebhookStatus> statuses;
}
