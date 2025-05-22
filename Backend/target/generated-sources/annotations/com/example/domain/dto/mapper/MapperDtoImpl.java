package com.example.domain.dto.mapper;

import com.example.database.model.company_subscription_module.company.Company;
import com.example.database.model.company_subscription_module.subscription.Subscription;
import com.example.database.model.company_subscription_module.user_roles.UserRole;
import com.example.database.model.company_subscription_module.user_roles.user.Gender;
import com.example.database.model.company_subscription_module.user_roles.user.User;
import com.example.database.model.company_subscription_module.user_roles.user.UserStatus;
import com.example.database.model.crm_module.client.Client;
import com.example.database.model.crm_module.client.TypeClient;
import com.example.domain.dto.ClientDto;
import com.example.domain.dto.CompanyDto;
import com.example.domain.dto.RegistrationDto;
import com.example.domain.dto.SubscriptionDto;
import com.example.domain.dto.UserDto;
import com.example.domain.dto.UserRoleDto;
import java.util.Date;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-05-22T15:31:21+0300",
    comments = "version: 1.6.0.Beta1, compiler: javac, environment: Java 21.0.7 (Microsoft)"
)
@Component
public class MapperDtoImpl implements MapperDto {

    @Override
    public Client toEntityClient(ClientDto clientDto) {
        if ( clientDto == null ) {
            return null;
        }

        Client client = new Client();

        client.setUser( toEntityUser( clientDto.getUserDto() ) );
        client.setId( clientDto.getId() );
        client.setName( clientDto.getName() );
        if ( clientDto.getTypeClient() != null ) {
            client.setTypeClient( Enum.valueOf( TypeClient.class, clientDto.getTypeClient() ) );
        }
        client.setTag( clientDto.getTag() );
        client.setCreatedAt( clientDto.getCreatedAt() );
        client.setUpdatedAt( clientDto.getUpdatedAt() );

        return client;
    }

    @Override
    public ClientDto toDtoClient(Client client) {
        if ( client == null ) {
            return null;
        }

        ClientDto clientDto = new ClientDto();

        clientDto.setUserDto( toDtoUser( client.getUser() ) );
        clientDto.setId( client.getId() );
        clientDto.setName( client.getName() );
        if ( client.getTypeClient() != null ) {
            clientDto.setTypeClient( client.getTypeClient().name() );
        }
        clientDto.setTag( client.getTag() );
        clientDto.setCreatedAt( client.getCreatedAt() );
        clientDto.setUpdatedAt( client.getUpdatedAt() );

        return clientDto;
    }

    @Override
    public Company toEntityCompany(CompanyDto companyDto) {
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

    @Override
    public CompanyDto toDtoCompany(Company company) {
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

    @Override
    public User toEntityUser(UserDto userDto) {
        if ( userDto == null ) {
            return null;
        }

        User user = new User();

        user.setCompany( toEntityCompany( userDto.getCompanyDto() ) );
        user.setId( userDto.getId() );
        user.setFullName( userDto.getFullName() );
        user.setEmail( userDto.getEmail() );
        if ( userDto.getStatus() != null ) {
            user.setStatus( Enum.valueOf( UserStatus.class, userDto.getStatus() ) );
        }
        user.setDateOfBirth( userDto.getDateOfBirth() );
        if ( userDto.getGender() != null ) {
            user.setGender( Enum.valueOf( Gender.class, userDto.getGender() ) );
        }
        user.setProfilePicture( userDto.getProfilePicture() );
        user.setCreatedAt( userDto.getCreatedAt() );
        user.setUpdatedAt( userDto.getUpdatedAt() );

        return user;
    }

    @Override
    public UserDto toDtoUser(User user) {
        if ( user == null ) {
            return null;
        }

        UserDto userDto = new UserDto();

        userDto.setCompanyDto( toDtoCompany( user.getCompany() ) );
        userDto.setId( user.getId() );
        userDto.setFullName( user.getFullName() );
        userDto.setEmail( user.getEmail() );
        if ( user.getStatus() != null ) {
            userDto.setStatus( user.getStatus().name() );
        }
        userDto.setDateOfBirth( user.getDateOfBirth() );
        if ( user.getGender() != null ) {
            userDto.setGender( user.getGender().name() );
        }
        userDto.setProfilePicture( user.getProfilePicture() );
        userDto.setCreatedAt( user.getCreatedAt() );
        userDto.setUpdatedAt( user.getUpdatedAt() );

        return userDto;
    }

    @Override
    public User toEntityUserFromRegistration(RegistrationDto registrationDto) {
        if ( registrationDto == null ) {
            return null;
        }

        User user = new User();

        user.setFullName( registrationDto.getFullName() );
        user.setEmail( registrationDto.getEmail() );
        user.setPassword( registrationDto.getPassword() );
        user.setCreatedAt( registrationDto.getCreatedAt() );
        user.setUpdatedAt( registrationDto.getUpdatedAt() );

        return user;
    }

    @Override
    public UserRole toEntityUserRole(UserRoleDto userRoleDto) {
        if ( userRoleDto == null ) {
            return null;
        }

        UserRole userRole = new UserRole();

        userRole.setRole( userRoleDto.getRole() );

        return userRole;
    }

    @Override
    public UserRoleDto toDtoUserRole(UserRole userRole) {
        if ( userRole == null ) {
            return null;
        }

        UserRoleDto userRoleDto = new UserRoleDto();

        userRoleDto.setRole( userRole.getRole() );

        return userRoleDto;
    }

    @Override
    public SubscriptionDto toSubscriptionDto(Subscription subscriptionDto) {
        if ( subscriptionDto == null ) {
            return null;
        }

        SubscriptionDto subscriptionDto1 = new SubscriptionDto();

        subscriptionDto1.setStatus( subscriptionDto.getStatus() );
        subscriptionDto1.setCost( subscriptionDto.getCost() );
        subscriptionDto1.setCountOperators( subscriptionDto.getCountOperators() );
        subscriptionDto1.setMaxOperators( subscriptionDto.getMaxOperators() );
        subscriptionDto1.setStartSubscription( subscriptionDto.getStartSubscription() );
        subscriptionDto1.setEndSubscription( subscriptionDto.getEndSubscription() );

        return subscriptionDto1;
    }
}
