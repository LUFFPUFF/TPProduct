package com.example.domain.api.company_api_test.service;

import com.example.database.model.company_subscription_module.user_roles.user.User;
import com.example.database.model.crm_module.client.Client;
import com.example.database.repository.company_subscription_module.UserRepository;
import com.example.database.repository.crm_module.ClientRepository;
import com.example.domain.dto.ClientDto;
import com.example.domain.dto.mapper.MapperDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ClientService {

    private final ClientRepository clientRepository;
    private final UserRepository userRepository;
    private final MapperDto mapperDto;

    public Client save(Client client) {
        return clientRepository.save(client);
    }

    public ClientDto createClient(ClientDto clientDto) {
        Client client = mapperDto.toEntityClient(clientDto);

        Client savedClient = clientRepository.save(client);

        return mapperDto.toDtoClient(savedClient);
    }

    public Optional<ClientDto> findByName(String name) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }

        return clientRepository.findByName(name).map(mapperDto::toDtoClient);
    }
}
