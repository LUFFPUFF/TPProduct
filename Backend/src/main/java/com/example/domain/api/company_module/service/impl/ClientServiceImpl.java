package com.example.domain.api.company_module.service.impl;

import com.example.database.model.company_subscription_module.company.Company;
import com.example.database.model.company_subscription_module.user_roles.user.User;
import com.example.database.model.crm_module.client.Client;
import com.example.database.model.crm_module.client.TypeClient;
import com.example.database.repository.company_subscription_module.CompanyRepository;
import com.example.database.repository.company_subscription_module.UserRepository;
import com.example.database.repository.crm_module.ClientRepository;
import com.example.domain.api.chat_service_api.exception_handler.ResourceNotFoundException;
import com.example.domain.api.chat_service_api.mapper.ClientMapper;
import com.example.domain.api.chat_service_api.model.dto.client.ClientDTO;
import com.example.domain.api.company_module.service.IClientService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ClientServiceImpl implements IClientService {

    private final ClientRepository clientRepository;
    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final ClientMapper clientMapper;

    public Client save(Client client) {
        return clientRepository.save(client);
    }

    @Override
    public Optional<Client> findById(Integer clientId) {
        return clientRepository.findById(clientId);
    }

    @Override
    public Optional<Client> findByName(String name) {
        return clientRepository.findByName(name);
    }

    @Override
    public Optional<Client> findByNameAndCompanyId(String name, Integer companyId) {
        return clientRepository.findByNameAndCompanyId(name, companyId);
    }

    @Override
    public Client findOrCreateTestClientForOperator(User operator) {
        String testClientName = "Тестовый клиент (Оператор " + operator.getFullName() + ")";
        Integer companyId = operator.getCompany().getId();

        return clientRepository.findByNameAndCompanyId(testClientName, companyId)
                .orElseGet(() -> {
                    Client newTestClient = new Client();
                    newTestClient.setName(testClientName);
                    newTestClient.setCompany(operator.getCompany());
                    return clientRepository.save(newTestClient);
                });
    }

    @Override
    public Optional<ClientDTO> findDtoById(Integer clientId) {
        return findById(clientId)
                .map(clientMapper::toDto);
    }

    @Override
    public Optional<Client> findClientEntityByTelegramUsername(String telegramUsername) {
        return clientRepository.findByName(telegramUsername);
    }

    @Override
    public Optional<Client> findClientEntityByEmail(String email) {
        return Optional.empty();
    }

    @Override
    public List<ClientDTO> getClientsByCompany(Integer companyId) {
        return clientRepository.findByCompanyId(companyId).stream()
                .map(clientMapper::toDto)
                .toList();
    }

    @Transactional
    public Client createClient(String name, Integer companyId, Integer userId) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company with ID " + companyId + " not found."));

        User user = null;
        if (userId != null) {
            user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User with ID " + userId + " not found."));
        }

        Client client = new Client();
        client.setName(name);
        client.setCompany(company);
        client.setUser(user);
        client.setCreatedAt(LocalDateTime.now());
        client.setUpdatedAt(LocalDateTime.now());
        client.setTypeClient(TypeClient.IMPORTANT);
        return clientRepository.save(client);

    }
}
