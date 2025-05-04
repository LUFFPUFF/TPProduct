package com.example.domain.api.chat_service_api.service;

import com.example.database.model.chats_messages_module.chat.Chat;
import com.example.database.model.crm_module.client.Client;
import com.example.domain.api.chat_service_api.model.dto.client.ClientDTO;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface IClientService {

    /**
     * Находит клиента по ID.
     * @param clientId ID клиента.
     * @return Optional Client Entity.
     */
    Optional<Client> findById(Integer clientId);

    Optional<Client> findByName(String name);

    Optional<Client> findByNameAndCompanyId(String name, Integer companyId);

    /**
     * Находит клиента по ID и возвращает DTO.
     * @param clientId ID клиента.
     * @return Optional ClientDTO.
     */
    Optional<ClientDTO> findDtoById(Integer clientId);

    Optional<Client> findClientEntityByTelegramUsername(String telegramUsername);

    Optional<Client> findClientEntityByEmail(String email);

    List<ClientDTO> getClientsByCompany(Integer companyId);

    Client createClient(String name, Integer companyId, Integer userId);
}
