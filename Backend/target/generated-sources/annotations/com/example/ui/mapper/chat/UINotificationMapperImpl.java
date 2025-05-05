package com.example.ui.mapper.chat;

import com.example.domain.api.chat_service_api.model.dto.ChatDTO;
import com.example.domain.api.chat_service_api.model.dto.notification.NotificationDTO;
import com.example.ui.dto.chat.notification.UiNotificationDto;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-05-05T22:26:03+0300",
    comments = "version: 1.6.0.Beta1, compiler: javac, environment: Java 21.0.7 (Microsoft)"
)
@Component
public class UINotificationMapperImpl implements UINotificationMapper {

    @Override
    public UiNotificationDto toUiDto(NotificationDTO notificationDTO) {
        if ( notificationDTO == null ) {
            return null;
        }

        UiNotificationDto.UiNotificationDtoBuilder uiNotificationDto = UiNotificationDto.builder();

        uiNotificationDto.chatId( notificationDTOChatId( notificationDTO ) );
        uiNotificationDto.id( notificationDTO.getId() );
        uiNotificationDto.type( notificationDTO.getType() );
        uiNotificationDto.message( notificationDTO.getMessage() );
        uiNotificationDto.createdAt( notificationDTO.getCreatedAt() );
        uiNotificationDto.read( notificationDTO.isRead() );

        return uiNotificationDto.build();
    }

    private Integer notificationDTOChatId(NotificationDTO notificationDTO) {
        ChatDTO chat = notificationDTO.getChat();
        if ( chat == null ) {
            return null;
        }
        return chat.getId();
    }
}
