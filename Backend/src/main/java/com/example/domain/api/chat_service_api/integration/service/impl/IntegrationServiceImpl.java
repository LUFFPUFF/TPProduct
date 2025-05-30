package com.example.domain.api.chat_service_api.integration.service.impl;

import com.example.database.model.company_subscription_module.company.*;
import com.example.database.model.company_subscription_module.user_roles.user.User;
import com.example.database.repository.company_subscription_module.*;
import com.example.domain.api.chat_service_api.exception_handler.ResourceNotFoundException;
import com.example.domain.api.chat_service_api.integration.dto.rest.*;
import com.example.domain.api.chat_service_api.integration.manager.mail.manager.EmailDialogManager;
import com.example.domain.api.chat_service_api.integration.manager.widget.DialogXChatManager;
import com.example.domain.api.chat_service_api.integration.service.IIntegrationService;
import com.example.domain.api.chat_service_api.integration.manager.telegram.TelegramBotManager;

import com.example.domain.api.chat_service_api.integration.manager.vk.VkBotManager;
import com.example.domain.security.model.UserContext;
import com.example.domain.security.util.UserContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.AccessDeniedException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class IntegrationServiceImpl implements IIntegrationService {

    private final CompanyTelegramConfigurationRepository companyTelegramConfigurationRepository;
    private final CompanyMailConfigurationRepository companyMailConfigurationRepository;
    private final CompanyWhatsappConfigurationRepository companyWhatsappConfigurationRepository;
    private final CompanyVkConfigurationRepository companyVkConfigurationRepository;
    private final CompanyDialogXChatConfigurationRepository companyDialogXChatConfigurationRepository;
    private final UserRepository userRepository;
    private final TelegramBotManager telegramBotManager;
    private final EmailDialogManager emailDialogManager;
    private final VkBotManager vkBotManager;
    private final DialogXChatManager dialogXChatManager;

    private User getUser() throws AccessDeniedException {
        UserContext userContext = UserContextHolder.getRequiredContext();
        if (userContext.getCompanyId() == null) {
            throw new AccessDeniedException("User is not associated with a company.");
        }
        return userRepository.findById(userContext.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User with id " + userContext.getUserId() + " not found"));
    }

    private Company getCompanyFromContext() throws AccessDeniedException {
        UserContext userContext = UserContextHolder.getRequiredContext();
        Integer companyId = userContext.getCompanyId();
        if (companyId == null) {
            throw new AccessDeniedException("User is not associated with a company.");
        }
        User user = getUser();
        if (user.getCompany() == null || !user.getCompany().getId().equals(companyId)) {
            throw new AccessDeniedException("User's company does not match context.");
        }
        return user.getCompany();
    }

    private void verifyCompanyAccess(Company company) throws AccessDeniedException {
        UserContext userContext = UserContextHolder.getRequiredContext();
        if (company == null || !company.getId().equals(userContext.getCompanyId())) {
            throw new AccessDeniedException("Access Denied: Configuration does not belong to the user's company.");
        }
    }

    @Override
    @Transactional
    public CompanyTelegramConfiguration createCompanyTelegramConfiguration(CreateTelegramConfigurationRequest request) throws AccessDeniedException {
        User userEntity = getUser();
        Company company = userEntity.getCompany();
        if (company == null) {
            throw new AccessDeniedException("User is not associated with a company.");
        }

        Optional<CompanyTelegramConfiguration> existingConfigOpt = companyTelegramConfigurationRepository
                .findByCompanyId(company.getId());

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
    public List<CompanyTelegramConfiguration> getAllTelegramConfigurations() throws AccessDeniedException {
        Company company = getCompanyFromContext();
        return companyTelegramConfigurationRepository.findAllByCompanyId(company.getId());
    }

    @Override
    public CompanyTelegramConfiguration getTelegramConfigurationById(Integer id) throws AccessDeniedException {
        CompanyTelegramConfiguration config = companyTelegramConfigurationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Could not find telegram configuration with id: " + id));
        verifyCompanyAccess(config.getCompany());
        return config;
    }

    @Override
    @Transactional
    public void deleteTelegramConfiguration(Integer id) throws AccessDeniedException {
        CompanyTelegramConfiguration config = getTelegramConfigurationById(id);
        Integer companyId = config.getCompany().getId();
        companyTelegramConfigurationRepository.delete(config);
        telegramBotManager.stopPollingForCompany(companyId);
    }

    @Override
    @Transactional
    public CompanyMailConfiguration createCompanyMailConfiguration(CreateMailConfigurationRequest request) throws AccessDeniedException {
        Company company = getCompanyFromContext();

        Optional<CompanyMailConfiguration> existingConfigOpt = companyMailConfigurationRepository
                .findByCompanyId(company.getId());

        CompanyMailConfiguration config;
        if (existingConfigOpt.isPresent()) {
            config = existingConfigOpt.get();
        } else {
            config = new CompanyMailConfiguration();
            config.setCompany(company);
            config.setCreatedAt(LocalDateTime.now());
        }

        config.setEmailAddress(request.getEmailAddress() != null ? request.getEmailAddress().trim() : null);
        config.setAppPassword(request.getAppPassword() != null ? request.getAppPassword().trim() : null);
        config.setImapServer(getImapServer(config.getEmailAddress()));
        config.setSmtpServer(getSmtpServer(config.getEmailAddress()));
        config.setImapPort(993);
        config.setFolder("INBOX");
        config.setUpdatedAt(LocalDateTime.now());

        CompanyMailConfiguration savedConfig = companyMailConfigurationRepository.save(config);
        emailDialogManager.startOrUpdatePollingForCompany(company.getId());
        return savedConfig;
    }

    @Override
    public List<CompanyMailConfiguration> getAllMailConfigurations() throws AccessDeniedException {
        Company company = getCompanyFromContext();
        return companyMailConfigurationRepository.findAllByCompanyId(company.getId());
    }

    @Override
    public CompanyMailConfiguration getMailConfigurationById(Integer id) throws AccessDeniedException {
        CompanyMailConfiguration config = companyMailConfigurationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Could not find mail configuration with id: " + id));
        verifyCompanyAccess(config.getCompany());
        return config;
    }

    @Override
    @Transactional
    public void deleteMailConfiguration(Integer id) throws AccessDeniedException {
        CompanyMailConfiguration config = getMailConfigurationById(id);
        Integer companyId = config.getCompany().getId();
        companyMailConfigurationRepository.delete(config);
        emailDialogManager.stopPollingForCompany(companyId);
    }

    @Override
    @Transactional
    public CompanyWhatsappConfiguration createCompanyWhatsappConfiguration(CreateWhatsappConfigurationRequest request) throws AccessDeniedException {
        Company company = getCompanyFromContext();

        Optional<CompanyWhatsappConfiguration> existingConfigOpt = companyWhatsappConfigurationRepository
                .findByCompanyId(company.getId());

        CompanyWhatsappConfiguration config;
        if (existingConfigOpt.isPresent()) {
            config = existingConfigOpt.get();
        } else {
            config = new CompanyWhatsappConfiguration();
            config.setCompany(company);
            config.setCreatedAt(LocalDateTime.now());
        }

        config.setAccessToken(request.getAccessToken());
        config.setVerifyToken(request.getVerifyToken());
        config.setPhoneNumberId(request.getPhoneNumberId());

        return companyWhatsappConfigurationRepository.save(config);
    }

    @Override
    public List<CompanyWhatsappConfiguration> getAllWhatsappConfigurations() throws AccessDeniedException {
        Company company = getCompanyFromContext();
        return companyWhatsappConfigurationRepository.findAllByCompanyId(company.getId());
    }

    @Override
    public CompanyWhatsappConfiguration getWhatsappConfigurationById(Integer id) throws AccessDeniedException {
        CompanyWhatsappConfiguration config = companyWhatsappConfigurationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Could not find WhatsApp configuration with id: " + id));
        verifyCompanyAccess(config.getCompany());
        return config;
    }

    @Override
    @Transactional
    public void deleteWhatsappConfiguration(Integer id) throws AccessDeniedException {
        companyWhatsappConfigurationRepository.delete(getWhatsappConfigurationById(id));
    }

    @Override
    @Transactional
    public CompanyVkConfiguration createCompanyVkConfiguration(CreateVkConfigurationRequest request) throws AccessDeniedException {
        Company company = getCompanyFromContext();

        Optional<CompanyVkConfiguration> existingConfigOpt = companyVkConfigurationRepository
                .findByCompanyId(company.getId());

        CompanyVkConfiguration config;
        if (existingConfigOpt.isPresent()) {
            config = existingConfigOpt.get();
        } else {
            config = new CompanyVkConfiguration();
            config.setCompany(company);
            config.setCreatedAt(LocalDateTime.now());
        }

        config.setCommunityId(request.getCommunityId());
        config.setCommunityName(request.getCommunityName());
        config.setAccessToken(request.getAccessToken());

        CompanyVkConfiguration savedConfig = companyVkConfigurationRepository.save(config);
        vkBotManager.startOrUpdatePollingForCompany(company.getId());
        return savedConfig;
    }

    @Override
    public List<CompanyVkConfiguration> getAllVkConfigurations() throws AccessDeniedException {
        Company company = getCompanyFromContext();
        return companyVkConfigurationRepository.findAllByCompanyId(company.getId());
    }

    @Override
    public CompanyVkConfiguration getVkConfigurationById(Integer id) throws AccessDeniedException {
        CompanyVkConfiguration config = companyVkConfigurationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Could not find VK configuration with id: " + id));
        verifyCompanyAccess(config.getCompany());
        return config;
    }

    @Override
    @Transactional
    public void deleteVkConfiguration(Integer id) throws AccessDeniedException {
        CompanyVkConfiguration config = getVkConfigurationById(id);
        Integer companyId = config.getCompany().getId();
        companyVkConfigurationRepository.delete(config);
        vkBotManager.stopPollingForCompany(companyId);
    }

    @Override
    @Transactional
    public CompanyDialogXChatConfiguration createOrUpdateCompanyDialogXChatConfiguration(CreateDialogXChatConfigurationRequest request) throws AccessDeniedException {
        Company company = getCompanyFromContext();

        CompanyDialogXChatConfiguration config = companyDialogXChatConfigurationRepository
                .findByCompanyId(company.getId())
                .orElseGet(() -> {
                    CompanyDialogXChatConfiguration newConfig = new CompanyDialogXChatConfiguration();
                    newConfig.setCompany(company);
                    return newConfig;
                });

        if (request.getEnabled() != null) {
            config.setEnabled(request.getEnabled());
        }
        if (request.getWelcomeMessage() != null) {
            config.setWelcomeMessage(request.getWelcomeMessage().trim());
        } else if (config.getId() == null) {
            config.setWelcomeMessage("Привет! Чем могу помочь?");
        }

        if (request.getThemeColor() != null) {
            config.setThemeColor(request.getThemeColor().trim());
        } else if (config.getId() == null) {
            config.setThemeColor("#5A38D9");
        }

        CompanyDialogXChatConfiguration savedConfig = companyDialogXChatConfigurationRepository.save(config);

        dialogXChatManager.processConfigurationChange(company.getId());

        return savedConfig;
    }

    @Override
    public CompanyDialogXChatConfiguration getDialogXChatConfigurationForCompany() throws AccessDeniedException {
        Company company = getCompanyFromContext();
        return companyDialogXChatConfigurationRepository.findByCompanyId(company.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "DialogX Chat configuration not found for company ID: " + company.getId()));
    }

    @Override
    @Transactional
    public void deleteDialogXChatConfigurationForCompany(Integer id) throws AccessDeniedException {
        CompanyDialogXChatConfiguration config = getDialogXChatConfigurationForCompany();
        companyDialogXChatConfigurationRepository.delete(config);
    }

    private String getSmtpServer(String emailAddress) {
        if (emailAddress == null) return null;
        if (emailAddress.contains("@gmail.com")) {
            return "smtp.gmail.com";
        } else if (emailAddress.contains("@yandex.ru") || emailAddress.contains("@ya.ru")) {
            return "smtp.yandex.com";
        }
        return null;
    }

    private String getImapServer(String emailAddress) {
        if (emailAddress == null) return null;
        if (emailAddress.contains("@gmail.com")) {
            return "imap.gmail.com";
        } else if (emailAddress.contains("@yandex.ru") || emailAddress.contains("@ya.ru")) {
            return "imap.yandex.com";
        }
        return null;
    }
}
