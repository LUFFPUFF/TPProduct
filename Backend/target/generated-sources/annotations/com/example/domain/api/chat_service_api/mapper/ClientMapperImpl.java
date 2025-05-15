package com.example.domain.api.chat_service_api.mapper;

import com.example.database.model.company_subscription_module.company.Company;
import com.example.database.model.company_subscription_module.user_roles.user.User;
import com.example.database.model.crm_module.client.Client;
import com.example.domain.api.chat_service_api.model.dto.client.ClientDTO;
import com.example.domain.api.chat_service_api.model.dto.client.ClientInfoDTO;
import com.example.domain.api.chat_service_api.model.dto.user.UserInfoDTO;
import com.example.domain.dto.CompanyDto;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-05-15T17:26:41+0300",
    comments = "version: 1.6.0.Beta1, compiler: javac, environment: Java 21.0.7 (Microsoft)"
)
@Component
public class ClientMapperImpl implements ClientMapper {

    @Override
    public ClientDTO toDto(Client client) {
        if ( client == null ) {
            return null;
        }

        ClientDTO clientDTO = new ClientDTO();

        clientDTO.setUser( userToUserInfoDTO( client.getUser() ) );
        clientDTO.setId( client.getId() );
        clientDTO.setCompany( companyToCompanyDto( client.getCompany() ) );
        clientDTO.setName( client.getName() );
        clientDTO.setTypeClient( client.getTypeClient() );
        clientDTO.setTag( client.getTag() );
        clientDTO.setCreatedAt( client.getCreatedAt() );
        clientDTO.setUpdatedAt( client.getUpdatedAt() );

        return clientDTO;
    }

    @Override
    public ClientInfoDTO toInfoDTO(Client client) {
        if ( client == null ) {
            return null;
        }

        ClientInfoDTO clientInfoDTO = new ClientInfoDTO();

        clientInfoDTO.setId( client.getId() );
        clientInfoDTO.setName( client.getName() );
        clientInfoDTO.setTag( client.getTag() );
        clientInfoDTO.setTypeClient( client.getTypeClient() );

        return clientInfoDTO;
    }

    @Override
    public Client toEntity(ClientDTO clientDTO) {
        if ( clientDTO == null ) {
            return null;
        }

        Client client = new Client();

        client.setUser( userInfoDTOToUser( clientDTO.getUser() ) );
        client.setCompany( companyDtoToCompany( clientDTO.getCompany() ) );
        client.setName( clientDTO.getName() );
        client.setTypeClient( clientDTO.getTypeClient() );
        client.setTag( clientDTO.getTag() );

        return client;
    }

    protected UserInfoDTO userToUserInfoDTO(User user) {
        if ( user == null ) {
            return null;
        }

        UserInfoDTO userInfoDTO = new UserInfoDTO();

        userInfoDTO.setId( user.getId() );
        userInfoDTO.setFullName( user.getFullName() );
        userInfoDTO.setProfilePicture( user.getProfilePicture() );
        userInfoDTO.setStatus( user.getStatus() );

        return userInfoDTO;
    }

    protected CompanyDto companyToCompanyDto(Company company) {
        if ( company == null ) {
            return null;
        }

        CompanyDto.CompanyDtoBuilder companyDto = CompanyDto.builder();

        companyDto.id( company.getId() );
        companyDto.name( company.getName() );
        companyDto.contactEmail( company.getContactEmail() );
        companyDto.createdAt( company.getCreatedAt() );
        companyDto.updatedAt( company.getUpdatedAt() );

        return companyDto.build();
    }

    protected User userInfoDTOToUser(UserInfoDTO userInfoDTO) {
        if ( userInfoDTO == null ) {
            return null;
        }

        User user = new User();

        user.setId( userInfoDTO.getId() );
        user.setFullName( userInfoDTO.getFullName() );
        user.setStatus( userInfoDTO.getStatus() );
        user.setProfilePicture( userInfoDTO.getProfilePicture() );

        return user;
    }

    protected Company companyDtoToCompany(CompanyDto companyDto) {
        if ( companyDto == null ) {
            return null;
        }

        Company company = new Company();

        company.setId( companyDto.getId() );
        company.setName( companyDto.getName() );
        company.setContactEmail( companyDto.getContactEmail() );
        company.setCreatedAt( companyDto.getCreatedAt() );
        company.setUpdatedAt( companyDto.getUpdatedAt() );

        return company;
    }
}
