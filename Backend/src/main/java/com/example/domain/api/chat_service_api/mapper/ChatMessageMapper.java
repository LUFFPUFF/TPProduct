package com.example.domain.api.chat_service_api.mapper;

import com.example.database.model.chats_messages_module.message.ChatMessage;
import com.example.database.model.company_subscription_module.user_roles.user.User;
import com.example.database.model.crm_module.client.Client;
import com.example.domain.api.chat_service_api.model.dto.MessageDto;
import com.example.domain.api.chat_service_api.model.dto.client.ClientInfoDTO;
import com.example.domain.api.chat_service_api.model.dto.user.UserInfoDTO;
import com.example.domain.api.chat_service_api.model.rest.mesage.SendMessageRequestDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring", uses = {ClientMapper.class, UserMapper.class})
public interface ChatMessageMapper {

    @Mapping(source = "chat", target = "chatDto")
    @Mapping(target = "senderClient", source = "senderClient", qualifiedByName = "mapClientInfo")
    @Mapping(target = "senderOperator", source = "senderOperator", qualifiedByName = "mapUserInfo")
    MessageDto toDto(ChatMessage message);

    @Named("mapClientInfo")
    default ClientInfoDTO mapClientInfo(Client client) {
        if (client == null) {
            return null;
        }
        return Mappers.getMapper(ClientMapper.class).toInfoDTO(client);
    }

    @Named("mapUserInfo")
    default UserInfoDTO mapUserInfo(User user) {
        if (user == null) {
            return null;
        }

        return Mappers.getMapper(UserMapper.class).toInfoDTO(user);
    }

     @Mapping(target = "id", ignore = true)
     @Mapping(target = "chat", ignore = true)
     @Mapping(target = "senderClient", ignore = true)
     @Mapping(target = "senderOperator", ignore = true)
     @Mapping(target = "sentAt", expression = "java(java.time.LocalDateTime.now())")
     @Mapping(target = "status", constant = "SENT")
     ChatMessage toEntity(SendMessageRequestDTO messageDTO);
}
