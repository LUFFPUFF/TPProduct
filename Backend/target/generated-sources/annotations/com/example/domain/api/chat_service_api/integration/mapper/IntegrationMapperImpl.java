package com.example.domain.api.chat_service_api.integration.mapper;

import com.example.database.model.company_subscription_module.company.CompanyMailConfiguration;
import com.example.database.model.company_subscription_module.company.CompanyTelegramConfiguration;
import com.example.domain.api.chat_service_api.integration.dto.IntegrationMailDto;
import com.example.domain.api.chat_service_api.integration.dto.IntegrationTelegramDto;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-05-21T16:13:57+0300",
    comments = "version: 1.6.0.Beta1, compiler: javac, environment: Java 21.0.7 (Microsoft)"
)
@Component
public class IntegrationMapperImpl implements IntegrationMapper {

    @Override
    public CompanyTelegramConfiguration toEntity(IntegrationTelegramDto integrationTelegramDto) {
        if ( integrationTelegramDto == null ) {
            return null;
        }

        CompanyTelegramConfiguration companyTelegramConfiguration = new CompanyTelegramConfiguration();

        companyTelegramConfiguration.setId( integrationTelegramDto.getId() );
        companyTelegramConfiguration.setChatTelegramId( integrationTelegramDto.getChatTelegramId() );
        companyTelegramConfiguration.setBotUsername( integrationTelegramDto.getBotUsername() );
        companyTelegramConfiguration.setBotToken( integrationTelegramDto.getBotToken() );
        companyTelegramConfiguration.setCreatedAt( integrationTelegramDto.getCreatedAt() );
        companyTelegramConfiguration.setUpdatedAt( integrationTelegramDto.getUpdatedAt() );

        return companyTelegramConfiguration;
    }

    @Override
    public IntegrationTelegramDto toDto(CompanyTelegramConfiguration companyTelegramConfiguration) {
        if ( companyTelegramConfiguration == null ) {
            return null;
        }

        IntegrationTelegramDto integrationTelegramDto = new IntegrationTelegramDto();

        integrationTelegramDto.setId( companyTelegramConfiguration.getId() );
        integrationTelegramDto.setChatTelegramId( companyTelegramConfiguration.getChatTelegramId() );
        integrationTelegramDto.setBotUsername( companyTelegramConfiguration.getBotUsername() );
        integrationTelegramDto.setBotToken( companyTelegramConfiguration.getBotToken() );
        integrationTelegramDto.setCreatedAt( companyTelegramConfiguration.getCreatedAt() );
        integrationTelegramDto.setUpdatedAt( companyTelegramConfiguration.getUpdatedAt() );

        return integrationTelegramDto;
    }

    @Override
    public CompanyMailConfiguration toEntity(IntegrationMailDto integrationMailDto) {
        if ( integrationMailDto == null ) {
            return null;
        }

        CompanyMailConfiguration companyMailConfiguration = new CompanyMailConfiguration();

        companyMailConfiguration.setId( integrationMailDto.getId() );
        companyMailConfiguration.setEmailAddress( integrationMailDto.getEmailAddress() );
        companyMailConfiguration.setAppPassword( integrationMailDto.getAppPassword() );
        companyMailConfiguration.setImapServer( integrationMailDto.getImapServer() );
        companyMailConfiguration.setCreatedAt( integrationMailDto.getCreatedAt() );
        companyMailConfiguration.setUpdatedAt( integrationMailDto.getUpdatedAt() );

        return companyMailConfiguration;
    }

    @Override
    public IntegrationMailDto toDto(CompanyMailConfiguration companyMailConfiguration) {
        if ( companyMailConfiguration == null ) {
            return null;
        }

        IntegrationMailDto integrationMailDto = new IntegrationMailDto();

        integrationMailDto.setId( companyMailConfiguration.getId() );
        integrationMailDto.setEmailAddress( companyMailConfiguration.getEmailAddress() );
        integrationMailDto.setAppPassword( companyMailConfiguration.getAppPassword() );
        integrationMailDto.setImapServer( companyMailConfiguration.getImapServer() );
        integrationMailDto.setCreatedAt( companyMailConfiguration.getCreatedAt() );
        integrationMailDto.setUpdatedAt( companyMailConfiguration.getUpdatedAt() );

        return integrationMailDto;
    }

    @Override
    public List<CompanyTelegramConfiguration> toEntity(List<IntegrationTelegramDto> integrationTelegramDtoList) {
        if ( integrationTelegramDtoList == null ) {
            return null;
        }

        List<CompanyTelegramConfiguration> list = new ArrayList<CompanyTelegramConfiguration>( integrationTelegramDtoList.size() );
        for ( IntegrationTelegramDto integrationTelegramDto : integrationTelegramDtoList ) {
            list.add( toEntity( integrationTelegramDto ) );
        }

        return list;
    }

    @Override
    public List<IntegrationTelegramDto> toDto(List<CompanyTelegramConfiguration> companyTelegramConfigurations) {
        if ( companyTelegramConfigurations == null ) {
            return null;
        }

        List<IntegrationTelegramDto> list = new ArrayList<IntegrationTelegramDto>( companyTelegramConfigurations.size() );
        for ( CompanyTelegramConfiguration companyTelegramConfiguration : companyTelegramConfigurations ) {
            list.add( toDto( companyTelegramConfiguration ) );
        }

        return list;
    }

    @Override
    public List<CompanyMailConfiguration> toEntityList(List<IntegrationMailDto> integrationMailDto) {
        if ( integrationMailDto == null ) {
            return null;
        }

        List<CompanyMailConfiguration> list = new ArrayList<CompanyMailConfiguration>( integrationMailDto.size() );
        for ( IntegrationMailDto integrationMailDto1 : integrationMailDto ) {
            list.add( toEntity( integrationMailDto1 ) );
        }

        return list;
    }

    @Override
    public List<IntegrationMailDto> toDtoList(List<CompanyMailConfiguration> companyMailConfiguration) {
        if ( companyMailConfiguration == null ) {
            return null;
        }

        List<IntegrationMailDto> list = new ArrayList<IntegrationMailDto>( companyMailConfiguration.size() );
        for ( CompanyMailConfiguration companyMailConfiguration1 : companyMailConfiguration ) {
            list.add( toDto( companyMailConfiguration1 ) );
        }

        return list;
    }
}
