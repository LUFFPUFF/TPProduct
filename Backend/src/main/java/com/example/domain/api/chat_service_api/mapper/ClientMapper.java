package com.example.domain.api.chat_service_api.mapper;

import com.example.database.model.crm_module.client.Client;
import com.example.domain.api.chat_service_api.model.dto.client.ClientDTO;
import com.example.domain.api.chat_service_api.model.dto.client.ClientInfoDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ClientMapper {

    @Mapping(target = "user", source = "user")
    ClientDTO toDto(Client client);

    ClientInfoDTO toInfoDTO(Client client);

     @Mapping(target = "id", ignore = true)
     @Mapping(target = "createdAt", ignore = true)
     @Mapping(target = "updatedAt", ignore = true)
     Client toEntity(ClientDTO clientDTO);

}
