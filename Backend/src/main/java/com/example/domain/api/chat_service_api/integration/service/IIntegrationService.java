package com.example.domain.api.chat_service_api.integration.service;

import com.example.database.model.company_subscription_module.company.*;
import com.example.domain.api.chat_service_api.integration.dto.rest.*;

import java.nio.file.AccessDeniedException;
import java.util.List;

public interface IIntegrationService {

    CompanyTelegramConfiguration createCompanyTelegramConfiguration(CreateTelegramConfigurationRequest request) throws AccessDeniedException;
    List<CompanyTelegramConfiguration> getAllTelegramConfigurations() throws AccessDeniedException;
    CompanyTelegramConfiguration getTelegramConfigurationById(Integer id) throws AccessDeniedException;
    void deleteTelegramConfiguration(Integer id) throws AccessDeniedException;

    CompanyMailConfiguration createCompanyMailConfiguration(CreateMailConfigurationRequest request) throws AccessDeniedException;
    List<CompanyMailConfiguration> getAllMailConfigurations() throws AccessDeniedException;
    CompanyMailConfiguration getMailConfigurationById(Integer id) throws AccessDeniedException;
    void deleteMailConfiguration(Integer id) throws AccessDeniedException;

    CompanyWhatsappConfiguration createCompanyWhatsappConfiguration(CreateWhatsappConfigurationRequest request) throws AccessDeniedException;
    List<CompanyWhatsappConfiguration> getAllWhatsappConfigurations() throws AccessDeniedException;
    CompanyWhatsappConfiguration getWhatsappConfigurationById(Integer id) throws AccessDeniedException;
    void deleteWhatsappConfiguration(Integer id) throws AccessDeniedException;

    CompanyVkConfiguration createCompanyVkConfiguration(CreateVkConfigurationRequest request) throws AccessDeniedException;
    List<CompanyVkConfiguration> getAllVkConfigurations() throws AccessDeniedException;
    CompanyVkConfiguration getVkConfigurationById(Integer id) throws AccessDeniedException;
    void deleteVkConfiguration(Integer id) throws AccessDeniedException;

    CompanyDialogXChatConfiguration createOrUpdateCompanyDialogXChatConfiguration(CreateDialogXChatConfigurationRequest request) throws AccessDeniedException;
    CompanyDialogXChatConfiguration getDialogXChatConfigurationForCompany() throws AccessDeniedException;
    void deleteDialogXChatConfigurationForCompany(Integer id) throws AccessDeniedException;
}
