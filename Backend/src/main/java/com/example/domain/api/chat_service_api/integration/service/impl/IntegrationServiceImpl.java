package com.example.domain.api.chat_service_api.integration.service.impl;

import com.example.database.model.company_subscription_module.company.CompanyMailConfiguration;
import com.example.database.model.company_subscription_module.company.CompanyTelegramConfiguration;
import com.example.database.model.company_subscription_module.user_roles.user.User;
import com.example.database.repository.company_subscription_module.CompanyMailConfigurationRepository;
import com.example.database.repository.company_subscription_module.CompanyTelegramConfigurationRepository;
import com.example.database.repository.company_subscription_module.UserRepository;
import com.example.domain.api.chat_service_api.exception_handler.ResourceNotFoundException;
import com.example.domain.api.chat_service_api.integration.dto.rest.CreateMailConfigurationRequest;
import com.example.domain.api.chat_service_api.integration.dto.rest.CreateTelegramConfigurationRequest;
import com.example.domain.api.chat_service_api.integration.mapper.IntegrationMapper;
import com.example.domain.api.chat_service_api.integration.service.IIntegrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class IntegrationServiceImpl implements IIntegrationService {

    private final CompanyTelegramConfigurationRepository companyTelegramConfigurationRepository;
    private final CompanyMailConfigurationRepository companyMailConfigurationRepository;
    private final UserRepository userRepository;
    private final IntegrationMapper integrationMapper;

    @Override
    public List<CompanyTelegramConfiguration> getAllTelegramConfigurations(Integer companyId) {
        return companyTelegramConfigurationRepository.findAllByCompanyId(companyId);
    }

    @Override
    public List<CompanyMailConfiguration> getAllMailConfigurations(Integer companyId) {
        return companyMailConfigurationRepository.findAllByCompanyId(companyId);
    }

    @Override
    public CompanyTelegramConfiguration getTelegramConfigurationById(Integer id) {
        return companyTelegramConfigurationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Could not find telegram configuration with id: " + id));
    }

    @Override
    public CompanyMailConfiguration getMailConfigurationById(Integer id) {
        return companyMailConfigurationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Could not find mail configuration with id: " + id));
    }

    @Override
    public CompanyTelegramConfiguration createCompanyTelegramConfiguration(CreateTelegramConfigurationRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        Optional<User> currentUserOpt = getCurrentAppUser(authentication.getName());

        User currentUser = currentUserOpt
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));


        CompanyTelegramConfiguration companyTelegramConfiguration = new CompanyTelegramConfiguration();
        companyTelegramConfiguration.setChatTelegramId(request.getChatId());
        companyTelegramConfiguration.setCompany(currentUser.getCompany());
        companyTelegramConfiguration.setBotUsername(request.getBotName());
        companyTelegramConfiguration.setBotToken(request.getBotToken());
        companyTelegramConfiguration.setCreatedAt(LocalDateTime.now());
        companyTelegramConfiguration.setUpdatedAt(LocalDateTime.now());
        companyTelegramConfigurationRepository.save(companyTelegramConfiguration);
        return companyTelegramConfiguration;
    }

    @Override
    public CompanyMailConfiguration createCompanyMailConfiguration(CreateMailConfigurationRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        Optional<User> currentUserOpt = getCurrentAppUser(authentication.getName());

        User currentUser = currentUserOpt
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        CompanyMailConfiguration companyMailConfiguration = new CompanyMailConfiguration();
        companyMailConfiguration.setCompany(currentUser.getCompany());
        companyMailConfiguration.setEmailAddress(request.getEmail());
        companyMailConfiguration.setAppPassword(request.getPassword());
        companyMailConfiguration.setImapServer(request.getImapHost());
        companyMailConfiguration.setImapPort(993);
        companyMailConfiguration.setFolder("INBOX");
        companyMailConfiguration.setCreatedAt(LocalDateTime.now());
        companyMailConfiguration.setUpdatedAt(LocalDateTime.now());
        companyMailConfigurationRepository.save(companyMailConfiguration);
        return companyMailConfiguration;

    }

    private Optional<User> getCurrentAppUser(String email) {
        return userRepository.findByEmail(email);
    }
}
