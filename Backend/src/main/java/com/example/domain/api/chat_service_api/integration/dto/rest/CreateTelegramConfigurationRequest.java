package com.example.domain.api.chat_service_api.integration.dto.rest;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CreateTelegramConfigurationRequest {
    private String botToken;
    private String botName;
    private Long chatId;
}
