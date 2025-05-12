package com.example.domain.api.chat_service_api.integration.service;

import com.example.database.model.company_subscription_module.company.CompanyMailConfiguration;
import com.example.database.model.company_subscription_module.company.CompanyTelegramConfiguration;
import com.example.domain.api.chat_service_api.integration.dto.rest.CreateMailConfigurationRequest;
import com.example.domain.api.chat_service_api.integration.dto.rest.CreateTelegramConfigurationRequest;

import java.util.List;

public interface IIntegrationService {

    List<CompanyTelegramConfiguration> getAllTelegramConfigurations();

    List<CompanyMailConfiguration> getAllMailConfigurations();

    CompanyTelegramConfiguration getTelegramConfigurationById(Integer id);

    CompanyMailConfiguration getMailConfigurationById(Integer id);

    CompanyTelegramConfiguration createCompanyTelegramConfiguration(CreateTelegramConfigurationRequest request);

    CompanyMailConfiguration createCompanyMailConfiguration(CreateMailConfigurationRequest request);


}
