package com.example.domain.api.chat_service_api.mapper;

import com.example.database.model.chats_messages_module.chat.Chat;
import com.example.database.model.chats_messages_module.chat.ChatStatus;
import com.example.database.model.chats_messages_module.message.ChatMessage;
import com.example.domain.api.chat_service_api.model.dto.ChatDTO;
import com.example.domain.api.chat_service_api.model.dto.ChatDetailsDTO;
import com.example.domain.api.chat_service_api.model.dto.MessageDto;
import com.example.domain.api.chat_service_api.model.rest.chat.CreateChatRequestDTO;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-05-02T01:48:54+0300",
    comments = "version: 1.6.0.Beta1, compiler: javac, environment: Java 21.0.7 (Microsoft)"
)
@Component
public class ChatMapperImpl implements ChatMapper {

    @Autowired
    private ChatMessageMapper chatMessageMapper;
    @Autowired
    private ClientMapper clientMapper;
    @Autowired
    private UserMapper userMapper;

    @Override
    public ChatDTO toDto(Chat chat) {
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

    @Override
    public ChatDetailsDTO toDetailsDto(Chat chat) {
        if ( chat == null ) {
            return null;
        }

        ChatDetailsDTO chatDetailsDTO = new ChatDetailsDTO();

        chatDetailsDTO.setClient( clientMapper.toInfoDTO( chat.getClient() ) );
        chatDetailsDTO.setOperator( userMapper.toInfoDTO( chat.getUser() ) );
        chatDetailsDTO.setMessages( chatMessageListToMessageDtoList( chat.getMessages() ) );
        chatDetailsDTO.setId( chat.getId() );
        chatDetailsDTO.setChatChannel( chat.getChatChannel() );
        chatDetailsDTO.setStatus( chat.getStatus() );
        chatDetailsDTO.setCreatedAt( chat.getCreatedAt() );
        chatDetailsDTO.setAssignedAt( chat.getAssignedAt() );
        chatDetailsDTO.setClosedAt( chat.getClosedAt() );
        chatDetailsDTO.setLastMessageAt( chat.getLastMessageAt() );

        return chatDetailsDTO;
    }

    @Override
    public Chat toEntity(CreateChatRequestDTO createChatDTO) {
        if ( createChatDTO == null ) {
            return null;
        }

        Chat chat = new Chat();

        chat.setChatChannel( createChatDTO.getChatChannel() );

        chat.setStatus( ChatStatus.PENDING_OPERATOR );
        chat.setCreatedAt( java.time.LocalDateTime.now() );
        chat.setLastMessageAt( java.time.LocalDateTime.now() );

        return chat;
    }

    protected List<MessageDto> chatMessageListToMessageDtoList(List<ChatMessage> list) {
        if ( list == null ) {
            return null;
        }

        List<MessageDto> list1 = new ArrayList<MessageDto>( list.size() );
        for ( ChatMessage chatMessage : list ) {
            list1.add( chatMessageMapper.toDto( chatMessage ) );
        }

        return list1;
    }
}
