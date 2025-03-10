package com.example.domain.api.company_api_test.service;

import com.example.database.model.crm_module.client.Client;
import com.example.database.repository.crm_module.ClientRepository;
import com.example.domain.dto.company_module.ClientDto;
import com.example.domain.dto.mapper.MapperDto;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ClientService {

    private ClientRepository clientRepository;
    private MapperDto mapperDto;

    public ClientDto createClient(ClientDto clientDto) {
        Client client = mapperDto.toEntityClient(clientDto);

        Client savedClient = clientRepository.save(client);

        return mapperDto.toDtoClient(savedClient);
    }

    public Optional<ClientDto> findByName(String name) {
        return clientRepository.findByName(name)
                .map(mapperDto::toDtoClient);
    }

    public Optional<Client> findByNameClient(String name) {
        return clientRepository.findByName(name);
    }
}
