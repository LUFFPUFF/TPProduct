package com.example.domain.api.chat_service_api.integration.dto;

import com.example.domain.dto.CompanyDto;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class IntegrationTelegramDto {

    private Integer id;
    private Long chatTelegramId;
    private CompanyDto companyDto;
    private String botUsername;
    private String botToken;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
