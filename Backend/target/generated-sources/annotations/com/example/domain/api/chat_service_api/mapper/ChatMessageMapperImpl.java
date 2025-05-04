package com.example.domain.api.chat_service_api.mapper;

import com.example.database.model.chats_messages_module.chat.Chat;
import com.example.database.model.chats_messages_module.message.ChatMessage;
import com.example.database.model.chats_messages_module.message.MessageStatus;
import com.example.domain.api.chat_service_api.model.dto.ChatDTO;
import com.example.domain.api.chat_service_api.model.dto.MessageDto;
import com.example.domain.api.chat_service_api.model.rest.mesage.SendMessageRequestDTO;
import javax.annotation.processing.Generated;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-05-04T00:54:45+0300",
    comments = "version: 1.6.0.Beta1, compiler: javac, environment: Java 21.0.7 (Microsoft)"
)
@Component
public class ChatMessageMapperImpl implements ChatMessageMapper {

    @Autowired
    private ClientMapper clientMapper;

    @Override
    public MessageDto toDto(ChatMessage message) {
        if ( message == null ) {
            return null;
        }

        MessageDto messageDto = new MessageDto();

        messageDto.setChatDto( chatToChatDTO( message.getChat() ) );
        messageDto.setSenderClient( mapClientInfo( message.getSenderClient() ) );
        messageDto.setSenderOperator( mapUserInfo( message.getSenderOperator() ) );
        messageDto.setId( message.getId() );
        messageDto.setContent( message.getContent() );
        messageDto.setSenderType( message.getSenderType() );
        messageDto.setStatus( message.getStatus() );
        messageDto.setReplyToExternalMessageId( message.getReplyToExternalMessageId() );
        messageDto.setSentAt( message.getSentAt() );

        return messageDto;
    }

    @Override
    public ChatMessage toEntity(SendMessageRequestDTO messageDTO) {
        if ( messageDTO == null ) {
            return null;
        }

        ChatMessage chatMessage = new ChatMessage();

        chatMessage.setContent( messageDTO.getContent() );
        chatMessage.setSenderType( messageDTO.getSenderType() );
        chatMessage.setExternalMessageId( messageDTO.getExternalMessageId() );
        chatMessage.setReplyToExternalMessageId( messageDTO.getReplyToExternalMessageId() );

        chatMessage.setSentAt( java.time.LocalDateTime.now() );
        chatMessage.setStatus( MessageStatus.SENT );

        return chatMessage;
    }

    protected ChatDTO chatToChatDTO(Chat chat) {
        if ( chat == null ) {
            return null;
        }

        ChatDTO chatDTO = new ChatDTO();

        chatDTO.setId( chat.getId() );
        chatDTO.setClient( clientMapper.toInfoDTO( chat.getClient() ) );
        chatDTO.setChatChannel( chat.getChatChannel() );
        chatDTO.setStatus( chat.getStatus() );
        chatDTO.setCreatedAt( chat.getCreatedAt() );
        chatDTO.setLastMessageAt( chat.getLastMessageAt() );

        return chatDTO;
    }
}
