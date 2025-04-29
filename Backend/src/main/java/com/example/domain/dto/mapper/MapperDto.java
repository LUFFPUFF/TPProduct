package com.example.domain.dto.mapper;

import com.example.database.model.chats_messages_module.ChatAttachment;
import com.example.database.model.chats_messages_module.ChatMessage;
import com.example.database.model.chats_messages_module.chat.Chat;
import com.example.database.model.company_subscription_module.Company;
import com.example.database.model.company_subscription_module.subscription.Subscription;
import com.example.database.model.company_subscription_module.company.Company;
import com.example.database.model.company_subscription_module.user_roles.UserRole;
import com.example.database.model.company_subscription_module.user_roles.user.Role;
import com.example.database.model.company_subscription_module.user_roles.user.User;
import com.example.database.model.crm_module.client.Client;
import com.example.domain.dto.chat_module.ChatAttachmentDto;
import com.example.domain.dto.chat_module.MessageDto;
import com.example.domain.dto.chat_module.ChatDto;
import com.example.domain.dto.company_module.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;


@Mapper(componentModel = "spring", imports = {java.util.Date.class}, uses = {ChatMapperHelper.class})
public interface MapperDto {
    //Subscription mapping
    //
    SubscriptionDto toSubscriptionDto(Subscription subscription);
    // Chat mapping
    @Mapping(source = "userDto", target = "user")
    @Mapping(source = "clientDto", target = "client")
    Chat toEntityChat(ChatDto chatDto);

    @Mapping(source = "user", target = "userDto")
    @Mapping(source = "client", target = "clientDto")
    ChatDto toDtoChat(Chat chat);

    // Message mapping
    @Mapping(source = "chatDto", target = "chat")
    @Mapping(source = "content", target = "content")
    @Mapping(source = "sentAt", target = "sentAt")
    ChatMessage toEntityChatMessage(MessageDto messageDto);

    @Mapping(source = "chat", target = "chatDto")
    @Mapping(source = "content", target = "content")
    @Mapping(source = "sentAt", target = "sentAt")
    MessageDto toDtoChatMessage(ChatMessage chatMessage);

    // ChatAttachment mapping
    ChatAttachment toEntityChatAttachment(ChatAttachmentDto chatAttachmentDto);
    ChatAttachmentDto toDtoChatAttachment(ChatAttachment chatAttachment);

    // Client mapping
    @Mapping(source = "userDto", target = "user")
    Client toEntityClient(ClientDto clientDto);
    @Mapping(source = "user", target = "userDto")
    ClientDto toDtoClient(Client client);

    // Company mapping
    Company toEntityCompany(CompanyDto companyDto);
    CompanyDto toDtoCompany(Company company);

    @Mapping(source = "companyDto", target = "company")
    User toEntityUser(UserDto userDto);
    @Mapping(source = "company", target = "companyDto")
    User toEntityUser(RegistrationDto registrationDto);
    UserDto toDtoUser(User user);

    // UserRole mapping
    UserRole toEntityUserRole(User user, Role role);
    UserRoleDto toDtoUserRole(UserRole userRole);


    // Преобразование LocalDateTime -> Date
    default Date map(LocalDateTime value) {
        return (value == null) ? null : Date.from(value.atZone(ZoneId.systemDefault()).toInstant());
    }

    // Преобразование Date -> LocalDateTime
    default LocalDateTime map(Date value) {
        return (value == null) ? null : value.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }
}

