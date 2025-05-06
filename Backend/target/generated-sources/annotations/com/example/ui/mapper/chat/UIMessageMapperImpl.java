package com.example.ui.mapper.chat;

import com.example.domain.api.chat_service_api.model.dto.MessageDto;
import com.example.ui.dto.chat.message.UiMessageDto;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-05-06T09:45:56+0300",
    comments = "version: 1.6.0.Beta1, compiler: javac, environment: Java 21.0.7 (Microsoft)"
)
@Component
public class UIMessageMapperImpl implements UIMessageMapper {

    @Override
    public UiMessageDto toUiDto(MessageDto messageDto) {
        if ( messageDto == null ) {
            return null;
        }

        UiMessageDto.UiMessageDtoBuilder uiMessageDto = UiMessageDto.builder();

        if ( messageDto.getSenderType() != null ) {
            uiMessageDto.senderType( messageDto.getSenderType().name() );
        }
        if ( messageDto.getStatus() != null ) {
            uiMessageDto.status( messageDto.getStatus().name() );
        }
        uiMessageDto.id( messageDto.getId() );
        uiMessageDto.content( messageDto.getContent() );
        uiMessageDto.sentAt( messageDto.getSentAt() );

        uiMessageDto.senderName( getSenderName(messageDto) );

        return uiMessageDto.build();
    }

    @Override
    public List<UiMessageDto> toUiDtoList(List<MessageDto> messageDtoList) {
        if ( messageDtoList == null ) {
            return null;
        }

        List<UiMessageDto> list = new ArrayList<UiMessageDto>( messageDtoList.size() );
        for ( MessageDto messageDto : messageDtoList ) {
            list.add( toUiDto( messageDto ) );
        }

        return list;
    }
}
