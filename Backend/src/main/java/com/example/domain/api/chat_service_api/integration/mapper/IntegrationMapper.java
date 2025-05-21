package com.example.domain.api.chat_service_api.integration.mapper;

import com.example.database.model.company_subscription_module.company.CompanyMailConfiguration;
import com.example.database.model.company_subscription_module.company.CompanyTelegramConfiguration;
import com.example.database.model.company_subscription_module.company.CompanyVkConfiguration;
import com.example.database.model.company_subscription_module.company.CompanyWhatsappConfiguration;
import com.example.domain.api.chat_service_api.integration.dto.IntegrationMailDto;
import com.example.domain.api.chat_service_api.integration.dto.IntegrationTelegramDto;
import com.example.domain.api.chat_service_api.integration.dto.IntegrationVkDto;
import com.example.domain.api.chat_service_api.integration.dto.IntegrationWhatsappDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface IntegrationMapper {

    @Mapping(source = "company", target = "companyDto")
    IntegrationTelegramDto toTelegramDto(CompanyTelegramConfiguration entity);

    List<IntegrationTelegramDto> toTelegramDtoList(List<CompanyTelegramConfiguration> entities);

    @Mapping(source = "company", target = "companyDto")
    IntegrationMailDto toMailDto(CompanyMailConfiguration entity);

    List<IntegrationMailDto> toMailDtoList(List<CompanyMailConfiguration> entities);

    @Mapping(source = "company", target = "companyDto")
    IntegrationWhatsappDto toWhatsappDto(CompanyWhatsappConfiguration entity);

    List<IntegrationWhatsappDto> toWhatsappDtoList(List<CompanyWhatsappConfiguration> entities);

    @Mapping(source = "company", target = "companyDto")
    @Mapping(target = "active", expression = "java(entity.isActive())")
    IntegrationVkDto toVkDto(CompanyVkConfiguration entity);

    List<IntegrationVkDto> toVkDtoList(List<CompanyVkConfiguration> entities);


}
