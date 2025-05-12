package com.example.domain.api.chat_service_api.integration.service.impl;

import com.example.database.model.company_subscription_module.company.Company;
import com.example.database.model.company_subscription_module.company.CompanyMailConfiguration;
import com.example.database.model.company_subscription_module.company.CompanyTelegramConfiguration;
import com.example.database.model.company_subscription_module.user_roles.user.User;
import com.example.database.repository.company_subscription_module.CompanyMailConfigurationRepository;
import com.example.database.repository.company_subscription_module.CompanyTelegramConfigurationRepository;
import com.example.database.repository.company_subscription_module.UserRepository;
import com.example.domain.api.chat_service_api.exception_handler.ResourceNotFoundException;
import com.example.domain.api.chat_service_api.integration.dto.rest.CreateMailConfigurationRequest;
import com.example.domain.api.chat_service_api.integration.dto.rest.CreateTelegramConfigurationRequest;
import com.example.domain.api.chat_service_api.integration.service.IIntegrationService;
import com.example.domain.api.chat_service_api.integration.telegram.TelegramBotManager;
import com.example.domain.security.UserContext;
import com.example.domain.security.UserContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class IntegrationServiceImpl implements IIntegrationService {

    private final CompanyTelegramConfigurationRepository companyTelegramConfigurationRepository;
    private final CompanyMailConfigurationRepository companyMailConfigurationRepository;
    private final UserRepository userRepository;
    private final TelegramBotManager telegramBotManager;
    private final UserContextHolder userContextHolder;

    @Override
    public List<CompanyTelegramConfiguration> getAllTelegramConfigurations() {
        UserContext userContext = userContextHolder.getUserContext();
        Optional<User> user = userRepository.findById(userContext.getUserId());

        if (user.isPresent()) {
            User userEntity = user.get();
            return companyTelegramConfigurationRepository.findAllByCompanyId(userEntity.getCompany().getId());
        }
        throw new ResourceNotFoundException("User with id " + userContext.getUserId() + " not found");
    }

    @Override
    public List<CompanyMailConfiguration> getAllMailConfigurations() {
        UserContext userContext = userContextHolder.getUserContext();
        Optional<User> user = userRepository.findById(userContext.getUserId());

        if (user.isPresent()) {
            User userEntity = user.get();

            return companyMailConfigurationRepository.findAllByCompanyId(userEntity.getCompany().getId());
        }

        throw new ResourceNotFoundException("User with id " + userContext.getUserId() + " not found");
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
    @Transactional
    public CompanyTelegramConfiguration createCompanyTelegramConfiguration(CreateTelegramConfigurationRequest request) {

        UserContext userContext = userContextHolder.getUserContext();
        Optional<User> user = userRepository.findById(userContext.getUserId());

        Company company = null;

        if (user.isPresent()) {
            User userEntity = user.get();
            company = userEntity.getCompany();
        }

        Optional<CompanyTelegramConfiguration> existingConfigOpt = companyTelegramConfigurationRepository
                .findByCompanyId(Objects.requireNonNull(company).getId());
        CompanyTelegramConfiguration config;

        if (existingConfigOpt.isPresent()) {
            config = existingConfigOpt.get();
        } else {
            config = new CompanyTelegramConfiguration();
            config.setCompany(company);
            config.setCreatedAt(LocalDateTime.now());
        }

        config.setBotUsername(request.getBotName() != null ? request.getBotName().trim() : null);
        config.setBotToken(request.getBotToken() != null ? request.getBotToken().trim() : null);
        config.setUpdatedAt(LocalDateTime.now());

        CompanyTelegramConfiguration savedConfig = companyTelegramConfigurationRepository.save(config);

        telegramBotManager.startOrUpdatePollingForCompany(company.getId());

        return savedConfig;
    }

    @Override
    @Transactional
    public CompanyMailConfiguration createCompanyMailConfiguration(CreateMailConfigurationRequest request) {
        UserContext userContext = userContextHolder.getUserContext();
        Optional<User> user = userRepository.findById(userContext.getUserId());

        Company company = null;

        if (user.isPresent()) {
            User userEntity = user.get();
            company = userEntity.getCompany();
        }


        Optional<CompanyMailConfiguration> existingConfigOpt = companyMailConfigurationRepository
                .findByCompanyId(Objects.requireNonNull(company).getId());
        CompanyMailConfiguration config;

        if (existingConfigOpt.isPresent()) {
            config = existingConfigOpt.get();
        } else {
            config = new CompanyMailConfiguration();
            config.setCompany(company);
            config.setCreatedAt(LocalDateTime.now());
        }

        config.setEmailAddress(request.getEmail() != null ? request.getEmail().trim() : null);
        config.setAppPassword(request.getPassword() != null ? request.getPassword().trim() : null);
        config.setImapServer(request.getImapHost() != null ? request.getImapHost().trim() : null);
        config.setImapPort(993);
        config.setFolder("INBOX");
        config.setUpdatedAt(LocalDateTime.now());


        return companyMailConfigurationRepository.save(config);
    }
}
