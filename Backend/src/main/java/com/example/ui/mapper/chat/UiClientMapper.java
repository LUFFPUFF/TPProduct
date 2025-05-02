package com.example.ui.mapper.chat;

import com.example.domain.api.chat_service_api.model.dto.client.ClientDTO;
import com.example.ui.dto.client.UiClientDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UiClientMapper {

    @Mapping(source = "typeClient", target = "type")
    UiClientDto toUiDto(ClientDTO clientDTO);
}
