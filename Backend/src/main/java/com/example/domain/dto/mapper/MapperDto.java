package com.example.domain.dto.mapper;

import com.example.database.model.chats_messages_module.ChatAttachment;
import com.example.database.model.chats_messages_module.ChatMessage;
import com.example.database.model.chats_messages_module.chat.Chat;
import com.example.database.model.company_subscription_module.company.Company;
import com.example.database.model.company_subscription_module.user_roles.UserRole;
import com.example.database.model.company_subscription_module.user_roles.role.Role;
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

    // Chat mapping
    Chat toEntityChat(ChatDto chatDto);
    ChatDto toDtoChat(Chat chat);

    // Message mapping
    @Mapping(source = "chatId", target = "chat")
    @Mapping(source = "content", target = "content")
    @Mapping(source = "sentAt", target = "sentAt")
    ChatMessage toEntityChatMessage(MessageDto messageDto);

    @Mapping(source = "chat.id", target = "chatId")
    @Mapping(source = "content", target = "content")
    @Mapping(source = "sentAt", target = "sentAt")
    MessageDto toDtoChatMessage(ChatMessage chatMessage);

    // ChatAttachment mapping
    ChatAttachment toEntityChatAttachment(ChatAttachmentDto chatAttachmentDto);
    ChatAttachmentDto toDtoChatAttachment(ChatAttachment chatAttachment);

    // Client mapping
    Client toEntityClient(ClientDto clientDto);
    ClientDto toDtoClient(Client client);

    // Company mapping
    Company toEntityCompany(CompanyDto companyDto);
    CompanyDto toDtoCompany(Company company);

    // User mapping
    User toEntityUser(UserDto userDto);
    UserDto toDtoUser(User user);

    // Role mapping
    Role toEntityRole(RoleDto roleDto);
    RoleDto toDtoRole(Role role);

    // UserRole mapping
    UserRole toEntityUserRole(UserRoleDto userRoleDto);
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

