package com.example.domain.api.chat_service_api.integration.dto.rest;

import com.example.database.model.company_subscription_module.company.Company;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CreateTelegramConfigurationRequest {
    private String botToken;
    private String botName;
    private Long chatId;
    private Company company;
}
