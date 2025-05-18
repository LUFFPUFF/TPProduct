package com.example.ui.mapper.chat;

import com.example.domain.api.chat_service_api.model.dto.ChatDTO;
import com.example.domain.api.chat_service_api.model.dto.ChatDetailsDTO;
import com.example.domain.api.chat_service_api.model.dto.client.ClientInfoDTO;
import com.example.domain.api.chat_service_api.model.dto.user.UserInfoDTO;
import com.example.ui.dto.chat.ChatUIDetailsDTO;
import com.example.ui.dto.chat.UIChatDto;
import com.example.ui.dto.client.UiClientDto;
import com.example.ui.dto.user.UiUserDto;
import javax.annotation.processing.Generated;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-05-18T13:36:38+0300",
    comments = "version: 1.6.0.Beta1, compiler: javac, environment: Java 22.0.2 (Amazon.com Inc.)"
)
@Component
public class UiChatMapperImpl implements UiChatMapper {

    @Autowired
    private UIMessageMapper uIMessageMapper;

    @Override
    public UIChatDto toUiDto(ChatDTO chatDTO) {
        if ( chatDTO == null ) {
            return null;
        }

        UIChatDto.UIChatDtoBuilder uIChatDto = UIChatDto.builder();

        uIChatDto.clientName( chatDTOClientName( chatDTO ) );
        if ( chatDTO.getChatChannel() != null ) {
            uIChatDto.source( chatDTO.getChatChannel().name() );
        }
        uIChatDto.lastMessageAt( chatDTO.getLastMessageAt() );
        uIChatDto.unreadCount( chatDTO.getUnreadMessagesCount() );
        if ( chatDTO.getStatus() != null ) {
            uIChatDto.status( chatDTO.getStatus().name() );
        }
        uIChatDto.assignedOperatorName( chatDTOOperatorFullName( chatDTO ) );
        uIChatDto.id( chatDTO.getId() );

        return uIChatDto.build();
    }

    @Override
    public ChatUIDetailsDTO toUiDetailsDto(ChatDetailsDTO chatDetailsDTO) {
        if ( chatDetailsDTO == null ) {
            return null;
        }

        ChatUIDetailsDTO.ChatUIDetailsDTOBuilder chatUIDetailsDTO = ChatUIDetailsDTO.builder();

        chatUIDetailsDTO.client( clientInfoDTOToUiClientDto( chatDetailsDTO.getClient() ) );
        chatUIDetailsDTO.assignedOperator( userInfoDTOToUiUserDto( chatDetailsDTO.getOperator() ) );
        if ( chatDetailsDTO.getChatChannel() != null ) {
            chatUIDetailsDTO.channel( chatDetailsDTO.getChatChannel().name() );
        }
        chatUIDetailsDTO.messages( uIMessageMapper.toUiDtoList( chatDetailsDTO.getMessages() ) );
        if ( chatDetailsDTO.getStatus() != null ) {
            chatUIDetailsDTO.status( chatDetailsDTO.getStatus().name() );
        }
        chatUIDetailsDTO.id( chatDetailsDTO.getId() );
        chatUIDetailsDTO.createdAt( chatDetailsDTO.getCreatedAt() );
        chatUIDetailsDTO.assignedAt( chatDetailsDTO.getAssignedAt() );
        chatUIDetailsDTO.closedAt( chatDetailsDTO.getClosedAt() );

        return chatUIDetailsDTO.build();
    }

    private String chatDTOClientName(ChatDTO chatDTO) {
        ClientInfoDTO client = chatDTO.getClient();
        if ( client == null ) {
            return null;
        }
        return client.getName();
    }

    private String chatDTOOperatorFullName(ChatDTO chatDTO) {
        UserInfoDTO operator = chatDTO.getOperator();
        if ( operator == null ) {
            return null;
        }
        return operator.getFullName();
    }

    protected UiClientDto clientInfoDTOToUiClientDto(ClientInfoDTO clientInfoDTO) {
        if ( clientInfoDTO == null ) {
            return null;
        }

        UiClientDto.UiClientDtoBuilder uiClientDto = UiClientDto.builder();

        uiClientDto.id( clientInfoDTO.getId() );
        uiClientDto.name( clientInfoDTO.getName() );
        uiClientDto.tag( clientInfoDTO.getTag() );

        return uiClientDto.build();
    }

    protected UiUserDto userInfoDTOToUiUserDto(UserInfoDTO userInfoDTO) {
        if ( userInfoDTO == null ) {
            return null;
        }

        UiUserDto.UiUserDtoBuilder uiUserDto = UiUserDto.builder();

        uiUserDto.id( userInfoDTO.getId() );
        uiUserDto.fullName( userInfoDTO.getFullName() );
        if ( userInfoDTO.getStatus() != null ) {
            uiUserDto.status( userInfoDTO.getStatus().name() );
        }
        uiUserDto.profilePicture( userInfoDTO.getProfilePicture() );

        return uiUserDto.build();
    }
}
