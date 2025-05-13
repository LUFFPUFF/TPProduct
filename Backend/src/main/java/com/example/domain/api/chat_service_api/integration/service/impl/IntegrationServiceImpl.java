package com.example.domain.api.chat_service_api.integration.service.impl;

import com.example.database.model.company_subscription_module.company.Company;
import com.example.database.model.company_subscription_module.company.CompanyMailConfiguration;
import com.example.database.model.company_subscription_module.company.CompanyTelegramConfiguration;
import com.example.database.model.company_subscription_module.user_roles.user.Role;
import com.example.database.model.company_subscription_module.user_roles.user.User;
import com.example.database.repository.company_subscription_module.CompanyMailConfigurationRepository;
import com.example.database.repository.company_subscription_module.CompanyTelegramConfigurationRepository;
import com.example.database.repository.company_subscription_module.UserRepository;
import com.example.domain.api.chat_service_api.exception_handler.ResourceNotFoundException;
import com.example.domain.api.chat_service_api.integration.dto.rest.CreateMailConfigurationRequest;
import com.example.domain.api.chat_service_api.integration.dto.rest.CreateTelegramConfigurationRequest;
import com.example.domain.api.chat_service_api.integration.service.IIntegrationService;
import com.example.domain.api.chat_service_api.integration.telegram.TelegramBotManager;
import com.example.domain.security.aop.annotation.RequireRole;
import com.example.domain.security.model.UserContext;
import com.example.domain.security.util.UserContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.AccessDeniedException;
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

    @Override
    @RequireRole(allowedRoles = {Role.MANAGER})
    public List<CompanyTelegramConfiguration> getAllTelegramConfigurations() throws AccessDeniedException {
        UserContext userContext = UserContextHolder.getRequiredContext();
        Integer companyId = userContext.getCompanyId();

        if (companyId == null) {
            throw new AccessDeniedException("User is not associated with a company.");
        }
        return companyTelegramConfigurationRepository.findAllByCompanyId(companyId);
    }

    @Override
    @RequireRole(allowedRoles = {Role.MANAGER})
    public List<CompanyMailConfiguration> getAllMailConfigurations() throws AccessDeniedException {
        UserContext userContext = UserContextHolder.getRequiredContext();
        Integer companyId = userContext.getCompanyId();


        if (companyId == null) {
            throw new AccessDeniedException("User is not associated with a company.");
        }

        return companyMailConfigurationRepository.findAllByCompanyId(companyId);
    }

    @Override
    public CompanyTelegramConfiguration getTelegramConfigurationById(Integer id) throws AccessDeniedException {
        CompanyTelegramConfiguration config = companyTelegramConfigurationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Could not find telegram configuration with id: " + id));

        UserContext userContext = UserContextHolder.getRequiredContext();
        if (config.getCompany() == null || !config.getCompany().getId().equals(userContext.getCompanyId())) {
            throw new AccessDeniedException("Access Denied: Configuration does not belong to the user's company.");
        }

        return config;
    }

    @Override
    public CompanyMailConfiguration getMailConfigurationById(Integer id) throws AccessDeniedException {
        CompanyMailConfiguration config = companyMailConfigurationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Could not find mail configuration with id: " + id));

        UserContext userContext = UserContextHolder.getRequiredContext();
        if (config.getCompany() == null || !config.getCompany().getId().equals(userContext.getCompanyId())) {
            throw new AccessDeniedException("Access Denied: Configuration does not belong to the user's company.");
        }

        return config;
    }

    @Override
    @Transactional
    @RequireRole(allowedRoles = {Role.MANAGER})
    public CompanyTelegramConfiguration createCompanyTelegramConfiguration(CreateTelegramConfigurationRequest request) throws AccessDeniedException {

        UserContext userContext = UserContextHolder.getRequiredContext();
        Integer companyId = userContext.getCompanyId();

        if (companyId == null) {
            throw new AccessDeniedException("User is not associated with a company.");
        }

        Optional<User> userOpt = userRepository.findById(userContext.getUserId());
        User userEntity = userOpt.orElseThrow(() -> new ResourceNotFoundException("User with id " + userContext.getUserId() + " not found"));
        Company company = userEntity.getCompany();

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

        config.setBotUsername(request.getBotUsername() != null ? request.getBotUsername().trim() : null);
        config.setBotToken(request.getBotToken() != null ? request.getBotToken().trim() : null);
        config.setUpdatedAt(LocalDateTime.now());

        CompanyTelegramConfiguration savedConfig = companyTelegramConfigurationRepository.save(config);
        telegramBotManager.startOrUpdatePollingForCompany(company.getId());

        return savedConfig;
    }

    @Override
    @Transactional
    @RequireRole(allowedRoles = {Role.MANAGER})
    public CompanyMailConfiguration createCompanyMailConfiguration(CreateMailConfigurationRequest request) throws AccessDeniedException {
        Company company = getCompany();

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

    private Company getCompany() throws AccessDeniedException {
        UserContext userContext = UserContextHolder.getRequiredContext();
        Integer companyId = userContext.getCompanyId();

        if (companyId == null) {
            throw new AccessDeniedException("User is not associated with a company.");
        }

        Optional<User> userOpt = userRepository.findById(userContext.getUserId());
        User userEntity = userOpt.orElseThrow(() -> new ResourceNotFoundException("User with id " + userContext.getUserId() + " not found"));
        return userEntity.getCompany();
    }
}
