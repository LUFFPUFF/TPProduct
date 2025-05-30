package com.example.domain.dto.mapper;

import com.example.database.model.chats_messages_module.ChatAttachment;
import com.example.database.model.chats_messages_module.message.ChatMessage;
import com.example.database.model.chats_messages_module.chat.Chat;
import com.example.database.model.company_subscription_module.company.Company;
import com.example.database.model.company_subscription_module.subscription.Subscription;
import com.example.database.model.company_subscription_module.user_roles.UserRole;
import com.example.database.model.company_subscription_module.user_roles.user.User;
import com.example.database.model.crm_module.client.Client;
import com.example.domain.api.chat_service_api.model.dto.ChatDTO;
import com.example.domain.dto.*;
import com.example.domain.api.chat_service_api.model.dto.MessageDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;


@Mapper(componentModel = "spring", imports = {java.util.Date.class}, uses = {ChatMapperHelper.class})
public interface MapperDto {

    @Mapping(source = "userDto", target = "user")
    Client toEntityClient(ClientDto clientDto);
    @Mapping(source = "user", target = "userDto")
    ClientDto toDtoClient(Client client);

    Company toEntityCompany(CompanyDto companyDto);
    CompanyDto toDtoCompany(Company company);

    @Mapping(source = "companyDto", target = "company")
    User toEntityUser(UserDto userDto);
    @Mapping(source = "company", target = "companyDto")
    UserDto toDtoUser(User user);

    User toEntityUserFromRegistration(RegistrationDto registrationDto);

    UserRole toEntityUserRole(UserRoleDto userRoleDto);
    UserRoleDto toDtoUserRole(UserRole userRole);

    SubscriptionDto toSubscriptionDto(Subscription subscriptionDto);


    // Преобразование LocalDateTime -> Date
    default Date map(LocalDateTime value) {
        return (value == null) ? null : Date.from(value.atZone(ZoneId.systemDefault()).toInstant());
    }

    // Преобразование Date -> LocalDateTime
    default LocalDateTime map(Date value) {
        return (value == null) ? null : value.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }
}

