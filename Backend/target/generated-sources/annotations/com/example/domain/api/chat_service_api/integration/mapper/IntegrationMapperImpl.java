package com.example.domain.api.chat_service_api.integration.mapper;

import com.example.database.model.company_subscription_module.company.Company;
import com.example.database.model.company_subscription_module.company.CompanyDialogXChatConfiguration;
import com.example.database.model.company_subscription_module.company.CompanyMailConfiguration;
import com.example.database.model.company_subscription_module.company.CompanyTelegramConfiguration;
import com.example.database.model.company_subscription_module.company.CompanyVkConfiguration;
import com.example.database.model.company_subscription_module.company.CompanyWhatsappConfiguration;
import com.example.domain.api.chat_service_api.integration.dto.DialogXChatDto;
import com.example.domain.api.chat_service_api.integration.dto.IntegrationMailDto;
import com.example.domain.api.chat_service_api.integration.dto.IntegrationTelegramDto;
import com.example.domain.api.chat_service_api.integration.dto.IntegrationVkDto;
import com.example.domain.api.chat_service_api.integration.dto.IntegrationWhatsappDto;
import com.example.domain.dto.CompanyDto;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-05-30T03:54:50+0300",
    comments = "version: 1.6.0.Beta1, compiler: javac, environment: Java 21.0.7 (Microsoft)"
)
@Component
public class IntegrationMapperImpl implements IntegrationMapper {

    @Override
    public IntegrationTelegramDto toTelegramDto(CompanyTelegramConfiguration entity) {
        if ( entity == null ) {
            return null;
        }

        IntegrationTelegramDto integrationTelegramDto = new IntegrationTelegramDto();

        integrationTelegramDto.setCompanyDto( companyToCompanyDto( entity.getCompany() ) );
        integrationTelegramDto.setId( entity.getId() );
        integrationTelegramDto.setChatTelegramId( entity.getChatTelegramId() );
        integrationTelegramDto.setBotUsername( entity.getBotUsername() );
        integrationTelegramDto.setBotToken( entity.getBotToken() );
        integrationTelegramDto.setCreatedAt( entity.getCreatedAt() );
        integrationTelegramDto.setUpdatedAt( entity.getUpdatedAt() );

        return integrationTelegramDto;
    }

    @Override
    public List<IntegrationTelegramDto> toTelegramDtoList(List<CompanyTelegramConfiguration> entities) {
        if ( entities == null ) {
            return null;
        }

        List<IntegrationTelegramDto> list = new ArrayList<IntegrationTelegramDto>( entities.size() );
        for ( CompanyTelegramConfiguration companyTelegramConfiguration : entities ) {
            list.add( toTelegramDto( companyTelegramConfiguration ) );
        }

        return list;
    }

    @Override
    public IntegrationMailDto toMailDto(CompanyMailConfiguration entity) {
        if ( entity == null ) {
            return null;
        }

        IntegrationMailDto integrationMailDto = new IntegrationMailDto();

        integrationMailDto.setCompanyDto( companyToCompanyDto( entity.getCompany() ) );
        integrationMailDto.setId( entity.getId() );
        integrationMailDto.setEmailAddress( entity.getEmailAddress() );
        integrationMailDto.setAppPassword( entity.getAppPassword() );
        integrationMailDto.setImapServer( entity.getImapServer() );
        integrationMailDto.setCreatedAt( entity.getCreatedAt() );
        integrationMailDto.setUpdatedAt( entity.getUpdatedAt() );

        return integrationMailDto;
    }

    @Override
    public List<IntegrationMailDto> toMailDtoList(List<CompanyMailConfiguration> entities) {
        if ( entities == null ) {
            return null;
        }

        List<IntegrationMailDto> list = new ArrayList<IntegrationMailDto>( entities.size() );
        for ( CompanyMailConfiguration companyMailConfiguration : entities ) {
            list.add( toMailDto( companyMailConfiguration ) );
        }

        return list;
    }

    @Override
    public IntegrationWhatsappDto toWhatsappDto(CompanyWhatsappConfiguration entity) {
        if ( entity == null ) {
            return null;
        }

        IntegrationWhatsappDto integrationWhatsappDto = new IntegrationWhatsappDto();

        integrationWhatsappDto.setCompanyDto( companyToCompanyDto( entity.getCompany() ) );
        integrationWhatsappDto.setId( entity.getId() );
        integrationWhatsappDto.setVerifyToken( entity.getVerifyToken() );
        integrationWhatsappDto.setCreatedAt( entity.getCreatedAt() );

        return integrationWhatsappDto;
    }

    @Override
    public List<IntegrationWhatsappDto> toWhatsappDtoList(List<CompanyWhatsappConfiguration> entities) {
        if ( entities == null ) {
            return null;
        }

        List<IntegrationWhatsappDto> list = new ArrayList<IntegrationWhatsappDto>( entities.size() );
        for ( CompanyWhatsappConfiguration companyWhatsappConfiguration : entities ) {
            list.add( toWhatsappDto( companyWhatsappConfiguration ) );
        }

        return list;
    }

    @Override
    public IntegrationVkDto toVkDto(CompanyVkConfiguration entity) {
        if ( entity == null ) {
            return null;
        }

        IntegrationVkDto integrationVkDto = new IntegrationVkDto();

        integrationVkDto.setCompanyDto( companyToCompanyDto( entity.getCompany() ) );
        integrationVkDto.setId( entity.getId() );
        integrationVkDto.setCommunityId( entity.getCommunityId() );
        integrationVkDto.setCommunityName( entity.getCommunityName() );
        integrationVkDto.setCreatedAt( entity.getCreatedAt() );

        integrationVkDto.setActive( entity.isActive() );

        return integrationVkDto;
    }

    @Override
    public List<IntegrationVkDto> toVkDtoList(List<CompanyVkConfiguration> entities) {
        if ( entities == null ) {
            return null;
        }

        List<IntegrationVkDto> list = new ArrayList<IntegrationVkDto>( entities.size() );
        for ( CompanyVkConfiguration companyVkConfiguration : entities ) {
            list.add( toVkDto( companyVkConfiguration ) );
        }

        return list;
    }

    @Override
    public DialogXChatDto toDto(CompanyDialogXChatConfiguration configuration) {
        if ( configuration == null ) {
            return null;
        }

        DialogXChatDto dialogXChatDto = new DialogXChatDto();

        dialogXChatDto.setCompanyId( configurationCompanyId( configuration ) );
        dialogXChatDto.setId( configuration.getId() );
        dialogXChatDto.setWidgetId( configuration.getWidgetId() );
        dialogXChatDto.setEnabled( configuration.isEnabled() );
        dialogXChatDto.setWelcomeMessage( configuration.getWelcomeMessage() );
        dialogXChatDto.setThemeColor( configuration.getThemeColor() );
        dialogXChatDto.setCreatedAt( configuration.getCreatedAt() );
        dialogXChatDto.setUpdatedAt( configuration.getUpdatedAt() );

        return dialogXChatDto;
    }

    protected CompanyDto companyToCompanyDto(Company company) {
        if ( company == null ) {
            return null;
        }

        CompanyDto.CompanyDtoBuilder companyDto = CompanyDto.builder();

        companyDto.id( company.getId() );
        companyDto.name( company.getName() );
        companyDto.contactEmail( company.getContactEmail() );
        companyDto.companyDescription( company.getCompanyDescription() );
        companyDto.createdAt( company.getCreatedAt() );
        companyDto.updatedAt( company.getUpdatedAt() );

        return companyDto.build();
    }

    private Integer configurationCompanyId(CompanyDialogXChatConfiguration companyDialogXChatConfiguration) {
        Company company = companyDialogXChatConfiguration.getCompany();
        if ( company == null ) {
            return null;
        }
        return company.getId();
    }
}
