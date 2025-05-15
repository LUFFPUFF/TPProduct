package com.example.domain.api.chat_service_api.integration.whats_app.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class WhatsappWebhookMetadata {

    @JsonProperty("display_phone_number")
    private String displayPhoneNumber;
    @JsonProperty("phone_number_id")
    private Long phoneNumberId;
}
