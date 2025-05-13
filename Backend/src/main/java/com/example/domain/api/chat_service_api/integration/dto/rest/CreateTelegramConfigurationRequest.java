package com.example.domain.api.chat_service_api.integration.dto.rest;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class CreateTelegramConfigurationRequest {

    @JsonProperty("botToken")
    private String botToken;

    @JsonProperty("botUsername")
    private String botUsername;

    @JsonProperty("chatId")
    private Long chatId;
}
