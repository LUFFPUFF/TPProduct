package com.example.domain.api.chat_service_api.integration.controller;

import com.example.database.model.company_subscription_module.company.CompanyMailConfiguration;
import com.example.database.model.company_subscription_module.company.CompanyTelegramConfiguration;
import com.example.database.model.company_subscription_module.user_roles.user.User;
import com.example.database.repository.company_subscription_module.UserRepository;
import com.example.domain.api.chat_service_api.exception_handler.ResourceNotFoundException;
import com.example.domain.api.chat_service_api.integration.dto.IntegrationMailDto;
import com.example.domain.api.chat_service_api.integration.dto.IntegrationTelegramDto;
import com.example.domain.api.chat_service_api.integration.dto.rest.CreateMailConfigurationRequest;
import com.example.domain.api.chat_service_api.integration.dto.rest.CreateTelegramConfigurationRequest;
import com.example.domain.api.chat_service_api.integration.mapper.IntegrationMapper;
import com.example.domain.api.chat_service_api.integration.service.IIntegrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/ui/integration")
@RequiredArgsConstructor
public class IntegrationController {

    private final IIntegrationService integrationService;
    private final IntegrationMapper integrationMapper;
    private final UserRepository userRepository;

    @PostMapping("/create/telegram")
    public ResponseEntity<IntegrationTelegramDto> createIntegrationTelegram(@RequestBody CreateTelegramConfigurationRequest request) {
        CompanyTelegramConfiguration companyTelegramConfiguration = integrationService.createCompanyTelegramConfiguration(request);
        return ResponseEntity.ok(integrationMapper.toDto(companyTelegramConfiguration));
    }

    @PostMapping("/create/email")
    public ResponseEntity<IntegrationMailDto> createIntegrationEmail(@RequestBody CreateMailConfigurationRequest request) {

        CompanyMailConfiguration companyMailConfiguration = integrationService.createCompanyMailConfiguration(request);
        return ResponseEntity.ok(integrationMapper.toDto(companyMailConfiguration));
    }

    @GetMapping("/telegram")
    public ResponseEntity<List<IntegrationTelegramDto>> getAllIntegrationTelegram() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Optional<User> currentUserOpt = getCurrentAppUser(authentication.getName());
        User currentUser = currentUserOpt
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        List<IntegrationTelegramDto> telegramDtos = integrationMapper.toDto(
                integrationService.getAllTelegramConfigurations(currentUser.getCompany().getId()));
        return ResponseEntity.ok(telegramDtos);
    }

    @GetMapping("/email")
    public ResponseEntity<List<IntegrationMailDto>> getAllIntegrationEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Optional<User> currentUserOpt = getCurrentAppUser(authentication.getName());
        User currentUser = currentUserOpt
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        List<IntegrationMailDto> mailDtos = integrationMapper.toDtoList(
                integrationService.getAllMailConfigurations(currentUser.getCompany().getId()));

        return ResponseEntity.ok(mailDtos);
    }

    private Optional<User> getCurrentAppUser(String email) {
        return userRepository.findByEmail(email);
    }

}
