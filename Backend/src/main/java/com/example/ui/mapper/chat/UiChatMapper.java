package com.example.ui.mapper.chat;

import com.example.domain.api.chat_service_api.model.dto.ChatDTO;
import com.example.domain.api.chat_service_api.model.dto.ChatDetailsDTO;
import com.example.domain.api.chat_service_api.model.dto.client.ClientDTO;
import com.example.domain.dto.UserDto;
import com.example.ui.dto.chat.ChatUIDetailsDTO;
import com.example.ui.dto.chat.UIChatDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring",
        uses = {UiClientMapper.class, UiUserMapper.class, UIMessageMapper.class})
public interface UiChatMapper {

    @Mapping(source = "client.name", target = "clientName")
    @Mapping(source = "chatChannel", target = "source")
    @Mapping(source = "lastMessageAt", target = "lastMessageAt")
    @Mapping(source = "unreadMessagesCount", target = "unreadCount")
    @Mapping(source = "status", target = "status")
    @Mapping(source = "operator.fullName", target = "assignedOperatorName")
    UIChatDto toUiDto(ChatDTO chatDTO);

    @Mapping(source = "client", target = "client")
    @Mapping(source = "operator", target = "assignedOperator")
    @Mapping(source = "chatChannel", target = "channel")
    @Mapping(source = "messages", target = "messages")
    @Mapping(source = "status", target = "status")
    ChatUIDetailsDTO toUiDetailsDto(ChatDetailsDTO chatDetailsDTO);

    @Named("mapClientDtoToName")
    default String mapClientDtoToName(ClientDTO clientDto) {
        return clientDto != null ? clientDto.getName() : null;
    }

    @Named("mapUserDtoToName")
    default String mapUserDtoToName(UserDto userDto) {
        return userDto != null ? userDto.getFullName() : null;
    }


}
