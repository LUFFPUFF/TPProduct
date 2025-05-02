package com.example.ui.mapper.chat;

import com.example.database.model.chats_messages_module.chat.ChatMessageSenderType;
import com.example.domain.api.chat_service_api.model.dto.MessageDto;
import com.example.ui.dto.chat.message.UiMessageDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface UIMessageMapper {

    @Mapping(target = "senderName", expression = "java(getSenderName(messageDto))")
    @Mapping(source = "senderType", target = "senderType")
    @Mapping(source = "status", target = "status")
    UiMessageDto toUiDto(MessageDto messageDto);

    List<UiMessageDto> toUiDtoList(List<MessageDto> messageDtoList);

    default String getSenderName(MessageDto messageDto) {
        if (messageDto == null) {
            return null;
        }

        if (messageDto.getSenderType() == ChatMessageSenderType.CLIENT) {
            return messageDto.getSenderClient() != null ? messageDto.getSenderClient().getName() : "Клиент";
        } else if (messageDto.getSenderType() == ChatMessageSenderType.OPERATOR) {
            return messageDto.getSenderOperator() != null ? messageDto.getSenderOperator().getFullName() : "Оператор";
        } else if (messageDto.getSenderType() == ChatMessageSenderType.AUTO_RESPONDER) {
            return "Автоответчик";
        }

        return "Неизвестно";
    }
}
