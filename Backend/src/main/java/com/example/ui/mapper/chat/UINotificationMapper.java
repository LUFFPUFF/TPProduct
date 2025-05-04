package com.example.ui.mapper.chat;

import com.example.domain.api.chat_service_api.model.dto.notification.NotificationDTO;
import com.example.ui.dto.chat.notification.UiNotificationDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UINotificationMapper {

    @Mapping(source = "chat.id", target = "chatId")
    UiNotificationDto toUiDto(NotificationDTO notificationDTO);
}
