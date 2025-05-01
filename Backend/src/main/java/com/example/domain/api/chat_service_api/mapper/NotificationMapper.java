package com.example.domain.api.chat_service_api.mapper;

import com.example.database.model.chats_messages_module.Notification;
import com.example.domain.api.chat_service_api.model.dto.notification.NotificationDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {ChatMapper.class})
public interface NotificationMapper {

    NotificationDTO toDto(Notification notification);

     @Mapping(target = "id", ignore = true)
     @Mapping(target = "user", ignore = true)
     @Mapping(target = "chat", ignore = true)
     @Mapping(target = "createdAt", expression = "java(java.time.LocalDateTime.now())")
     @Mapping(target = "read", constant = "false")
     Notification toEntity(NotificationDTO notificationDTO);
}
