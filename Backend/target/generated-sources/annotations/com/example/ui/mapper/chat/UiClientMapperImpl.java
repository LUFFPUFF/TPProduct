package com.example.ui.mapper.chat;

import com.example.domain.api.chat_service_api.model.dto.client.ClientDTO;
import com.example.ui.dto.client.UiClientDto;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-05-17T19:12:18+0300",
    comments = "version: 1.6.0.Beta1, compiler: javac, environment: Java 17.0.14 (Amazon.com Inc.)"
)
@Component
public class UiClientMapperImpl implements UiClientMapper {

    @Override
    public UiClientDto toUiDto(ClientDTO clientDTO) {
        if ( clientDTO == null ) {
            return null;
        }

        UiClientDto.UiClientDtoBuilder uiClientDto = UiClientDto.builder();

        if ( clientDTO.getTypeClient() != null ) {
            uiClientDto.type( clientDTO.getTypeClient().name() );
        }
        uiClientDto.id( clientDTO.getId() );
        uiClientDto.name( clientDTO.getName() );
        uiClientDto.tag( clientDTO.getTag() );

        return uiClientDto.build();
    }
}
