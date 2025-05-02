package com.example.domain.api.chat_service_api.mapper;

import com.example.database.model.chats_messages_module.Notification;
import com.example.domain.api.chat_service_api.model.dto.notification.NotificationDTO;
import javax.annotation.processing.Generated;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-05-02T17:02:48+0300",
    comments = "version: 1.6.0.Beta1, compiler: javac, environment: Java 21.0.7 (Microsoft)"
)
@Component
public class NotificationMapperImpl implements NotificationMapper {

    @Autowired
    private ChatMapper chatMapper;

    @Override
    public NotificationDTO toDto(Notification notification) {
        if ( notification == null ) {
            return null;
        }

        NotificationDTO notificationDTO = new NotificationDTO();

        notificationDTO.setId( notification.getId() );
        notificationDTO.setChat( chatMapper.toDto( notification.getChat() ) );
        notificationDTO.setType( notification.getType() );
        notificationDTO.setMessage( notification.getMessage() );
        notificationDTO.setCreatedAt( notification.getCreatedAt() );
        notificationDTO.setRead( notification.isRead() );

        return notificationDTO;
    }

    @Override
    public Notification toEntity(NotificationDTO notificationDTO) {
        if ( notificationDTO == null ) {
            return null;
        }

        Notification notification = new Notification();

        notification.setType( notificationDTO.getType() );
        notification.setMessage( notificationDTO.getMessage() );

        notification.setCreatedAt( java.time.LocalDateTime.now() );
        notification.setRead( false );

        return notification;
    }
}
