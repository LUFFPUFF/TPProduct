package com.example.domain.api.chat_service_api.integration.service;

import com.example.database.model.company_subscription_module.company.CompanyMailConfiguration;
import com.example.database.model.company_subscription_module.company.CompanyTelegramConfiguration;
import com.example.domain.api.chat_service_api.integration.dto.rest.CreateMailConfigurationRequest;
import com.example.domain.api.chat_service_api.integration.dto.rest.CreateTelegramConfigurationRequest;

import java.nio.file.AccessDeniedException;
import java.util.List;

public interface IIntegrationService {

    List<CompanyTelegramConfiguration> getAllTelegramConfigurations() throws AccessDeniedException;

    List<CompanyMailConfiguration> getAllMailConfigurations() throws AccessDeniedException;

    CompanyTelegramConfiguration getTelegramConfigurationById(Integer id) throws AccessDeniedException;

    CompanyMailConfiguration getMailConfigurationById(Integer id) throws AccessDeniedException;

    CompanyTelegramConfiguration createCompanyTelegramConfiguration(CreateTelegramConfigurationRequest request) throws AccessDeniedException;

    CompanyMailConfiguration createCompanyMailConfiguration(CreateMailConfigurationRequest request) throws AccessDeniedException;


}
