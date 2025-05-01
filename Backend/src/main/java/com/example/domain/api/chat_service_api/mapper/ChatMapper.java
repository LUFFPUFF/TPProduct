package com.example.domain.api.chat_service_api.mapper;

import com.example.database.model.chats_messages_module.chat.Chat;
import com.example.domain.api.chat_service_api.model.dto.ChatDTO;
import com.example.domain.api.chat_service_api.model.dto.ChatDetailsDTO;
import com.example.domain.api.chat_service_api.model.rest.chat.CreateChatRequestDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {ChatMessageMapper.class, ClientMapper.class, UserMapper.class})
public interface ChatMapper {

    ChatDTO toDto(Chat chat);

    @Mapping(target = "client", source = "client")
    @Mapping(target = "operator", source = "user")
    @Mapping(target = "messages", source = "messages")
    ChatDetailsDTO toDetailsDto(Chat chat);

     @Mapping(target = "id", ignore = true)
     @Mapping(target = "company", ignore = true)
     @Mapping(target = "client", ignore = true)
     @Mapping(target = "user", ignore = true)
     @Mapping(target = "status", constant = "PENDING_OPERATOR")
     @Mapping(target = "createdAt", expression = "java(java.time.LocalDateTime.now())")
     @Mapping(target = "assignedAt", ignore = true)
     @Mapping(target = "closedAt", ignore = true)
     @Mapping(target = "lastMessageAt", expression = "java(java.time.LocalDateTime.now())")
     @Mapping(target = "messages", ignore = true)
     Chat toEntity(CreateChatRequestDTO createChatDTO);
}
