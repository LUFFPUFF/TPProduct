package com.example.domain.api.chat_service_api.integration.whats_app.model.request;

import com.example.domain.api.chat_service_api.integration.whats_app.model.WhatsappWebhookEntry;
import lombok.Data;

import java.util.List;

@Data
public class WhatsappWebhookRequest {

    private String object;
    private List<WhatsappWebhookEntry> entry;
}
