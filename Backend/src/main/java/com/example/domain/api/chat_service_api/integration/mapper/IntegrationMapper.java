package com.example.domain.api.chat_service_api.integration.mapper;

import com.example.database.model.company_subscription_module.company.CompanyMailConfiguration;
import com.example.database.model.company_subscription_module.company.CompanyTelegramConfiguration;
import com.example.domain.api.chat_service_api.integration.dto.IntegrationMailDto;
import com.example.domain.api.chat_service_api.integration.dto.IntegrationTelegramDto;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface IntegrationMapper {

    CompanyTelegramConfiguration toEntity(IntegrationTelegramDto integrationTelegramDto);

    IntegrationTelegramDto toDto(CompanyTelegramConfiguration companyTelegramConfiguration);

    CompanyMailConfiguration toEntity(IntegrationMailDto integrationMailDto);

    IntegrationMailDto toDto(CompanyMailConfiguration companyMailConfiguration);

    List<CompanyTelegramConfiguration> toEntity(List<IntegrationTelegramDto> integrationTelegramDtoList);

    List<IntegrationTelegramDto> toDto(List<CompanyTelegramConfiguration> companyTelegramConfigurations);

    List<CompanyMailConfiguration> toEntityList( List<IntegrationMailDto> integrationMailDto);

    List<IntegrationMailDto> toDtoList(List<CompanyMailConfiguration> companyMailConfiguration);

}
