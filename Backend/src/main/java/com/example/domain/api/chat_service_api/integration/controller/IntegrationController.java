package com.example.domain.api.chat_service_api.integration.controller;

import com.example.database.model.company_subscription_module.company.CompanyMailConfiguration;
import com.example.database.model.company_subscription_module.company.CompanyTelegramConfiguration;
import com.example.domain.api.chat_service_api.integration.dto.IntegrationMailDto;
import com.example.domain.api.chat_service_api.integration.dto.IntegrationTelegramDto;
import com.example.domain.api.chat_service_api.integration.dto.rest.CreateMailConfigurationRequest;
import com.example.domain.api.chat_service_api.integration.dto.rest.CreateTelegramConfigurationRequest;
import com.example.domain.api.chat_service_api.integration.mapper.IntegrationMapper;
import com.example.domain.api.chat_service_api.integration.service.IIntegrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.AccessDeniedException;
import java.util.List;

@RestController
@RequestMapping("/api/ui/integration")
@RequiredArgsConstructor
public class IntegrationController {

    private final IIntegrationService integrationService;
    private final IntegrationMapper integrationMapper;

    @PostMapping("/create/telegram")
    public ResponseEntity<IntegrationTelegramDto> createIntegrationTelegram(@RequestBody CreateTelegramConfigurationRequest request) {
        CompanyTelegramConfiguration companyTelegramConfiguration = null;
        try {
            companyTelegramConfiguration = integrationService.createCompanyTelegramConfiguration(request);
        } catch (AccessDeniedException e) {
            throw new RuntimeException(e);
        }
        return ResponseEntity.ok(integrationMapper.toDto(companyTelegramConfiguration));
    }

    @PostMapping("/create/email")
    public ResponseEntity<IntegrationMailDto> createIntegrationEmail(@RequestBody CreateMailConfigurationRequest request) {
        CompanyMailConfiguration companyMailConfiguration = null;
        try {
            companyMailConfiguration = integrationService.createCompanyMailConfiguration(request);
        } catch (AccessDeniedException e) {
            throw new RuntimeException(e);
        }
        return ResponseEntity.ok(integrationMapper.toDto(companyMailConfiguration));
    }

    @GetMapping("/telegram")
    public ResponseEntity<List<IntegrationTelegramDto>> getAllIntegrationTelegram() {
        List<IntegrationTelegramDto> telegramDtos = null;
        try {
            telegramDtos = integrationMapper.toDto(
                    integrationService.getAllTelegramConfigurations());
        } catch (AccessDeniedException e) {
            throw new RuntimeException(e);
        }
        return ResponseEntity.ok(telegramDtos);
    }

    @GetMapping("/email")
    public ResponseEntity<List<IntegrationMailDto>> getAllIntegrationEmail() {
        List<IntegrationMailDto> mailDtos = null;
        try {
            mailDtos = integrationMapper.toDtoList(
                    integrationService.getAllMailConfigurations());
        } catch (AccessDeniedException e) {
            throw new RuntimeException(e);
        }

        return ResponseEntity.ok(mailDtos);
    }
}
