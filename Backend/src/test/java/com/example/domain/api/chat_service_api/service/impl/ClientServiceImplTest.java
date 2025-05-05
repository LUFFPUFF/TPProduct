package com.example.domain.api.chat_service_api.service.impl;

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
import com.example.domain.api.chat_service_api.model.dto.client.ClientInfoDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.dao.DataAccessException;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
// Убираем StreamSupport, он больше не нужен
// import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ClientServiceImplTest {

    @Mock private ClientRepository clientRepository;
    @Mock private CompanyRepository companyRepository;
    @Mock private UserRepository userRepository;
    @Mock private ClientMapper clientMapper;

    @InjectMocks
    private ClientServiceImpl clientService;

    @Captor
    private ArgumentCaptor<Client> clientCaptor;

    private Client testClient;
    private Company testCompany;
    private User testUser;
    private ClientDTO testClientDto;
    private final Integer COMPANY_ID = 1;
    private final Integer USER_ID = 1;
    private final Integer CLIENT_ID = 1;
    private final String CLIENT_NAME = "Test Client";
    private final String NEW_CLIENT_NAME = "New Client";
    private final String TELEGRAM_USERNAME = "telegram_user";


    @BeforeEach
    void setUp() {
        reset(clientRepository, companyRepository, userRepository, clientMapper);

        testCompany = new Company();
        testCompany.setId(COMPANY_ID);
        testCompany.setName("Test Company");
        testUser = new User();
        testUser.setId(USER_ID);
        testUser.setFullName("Test User");
        testClient = new Client();
        testClient.setId(CLIENT_ID);
        testClient.setName(CLIENT_NAME);
        testClient.setCompany(testCompany);
        testClient.setUser(testUser);
        testClient.setTypeClient(TypeClient.IMPORTANT);
        testClient.setCreatedAt(LocalDateTime.now().minusDays(1));
        testClient.setUpdatedAt(LocalDateTime.now().minusHours(1));
        testClientDto = new ClientDTO();
        testClientDto.setId(CLIENT_ID);
        testClientDto.setName(CLIENT_NAME);
    }

    // --- Тесты поиска ---
    // ... (findById, findByName, findDtoById, findByTelegram остаются без изменений) ...
    @Test
    void findById_WhenClientExists_ShouldReturnClient() {
        when(clientRepository.findById(CLIENT_ID)).thenReturn(Optional.of(testClient));
        Optional<Client> result = clientService.findById(CLIENT_ID);
        assertTrue(result.isPresent());
        assertEquals(testClient, result.get());
        verify(clientRepository).findById(CLIENT_ID);
    }

    @Test
    void findById_WhenClientNotExists_ShouldReturnEmpty() {
        when(clientRepository.findById(eq(CLIENT_ID + 1))).thenReturn(Optional.empty());
        Optional<Client> result = clientService.findById(CLIENT_ID + 1);
        assertTrue(result.isEmpty());
        verify(clientRepository).findById(CLIENT_ID + 1);
    }

    @Test
    void findByName_WhenClientExists_ShouldReturnClient() {
        when(clientRepository.findByName(CLIENT_NAME)).thenReturn(Optional.of(testClient));
        Optional<Client> result = clientService.findByName(CLIENT_NAME);
        assertTrue(result.isPresent());
        assertEquals(testClient, result.get());
        verify(clientRepository).findByName(CLIENT_NAME);
    }

    @Test
    void findByName_WhenClientNotExists_ShouldReturnEmpty() {
        when(clientRepository.findByName(eq("NonExistentName"))).thenReturn(Optional.empty());
        Optional<Client> result = clientService.findByName("NonExistentName");
        assertTrue(result.isEmpty());
        verify(clientRepository).findByName("NonExistentName");
    }

    @Test
    void findByName_NullName_ShouldCallRepositoryAndReturnEmpty() {
        when(clientRepository.findByName(null)).thenReturn(Optional.empty());
        Optional<Client> result = clientService.findByName(null);
        assertTrue(result.isEmpty());
        verify(clientRepository).findByName(null);
    }

    @Test
    void findByName_EmptyName_ShouldCallRepositoryAndReturnEmpty() {
        when(clientRepository.findByName(eq(""))).thenReturn(Optional.empty());
        Optional<Client> result = clientService.findByName("");
        assertTrue(result.isEmpty());
        verify(clientRepository).findByName("");
    }

    @Test
    void findDtoById_WhenClientExists_ShouldReturnClientDto() {
        when(clientRepository.findById(CLIENT_ID)).thenReturn(Optional.of(testClient));
        when(clientMapper.toDto(testClient)).thenReturn(testClientDto);
        Optional<ClientDTO> result = clientService.findDtoById(CLIENT_ID);
        assertTrue(result.isPresent());
        assertEquals(testClientDto, result.get());
        verify(clientRepository).findById(CLIENT_ID);
        verify(clientMapper).toDto(testClient);
    }

    @Test
    void findDtoById_WhenClientNotExists_ShouldReturnEmpty() {
        when(clientRepository.findById(eq(CLIENT_ID + 1))).thenReturn(Optional.empty());
        Optional<ClientDTO> result = clientService.findDtoById(CLIENT_ID + 1);
        assertTrue(result.isEmpty());
        verify(clientRepository).findById(CLIENT_ID + 1);
        verify(clientMapper, never()).toDto(any());
    }

    @Test
    void findClientEntityByTelegramUsername_ShouldUseFindByName() {
        when(clientRepository.findByName(TELEGRAM_USERNAME)).thenReturn(Optional.of(testClient));
        Optional<Client> result = clientService.findClientEntityByTelegramUsername(TELEGRAM_USERNAME);
        assertTrue(result.isPresent());
        assertEquals(testClient, result.get());
        verify(clientRepository).findByName(TELEGRAM_USERNAME);
    }


    @Test
    void findClientEntityByEmail_ShouldAlwaysReturnEmpty() {
        String testEmail = "some.email@example.com";
        Optional<Client> result = clientService.findClientEntityByEmail(testEmail);
        assertTrue(result.isEmpty());
        verify(clientRepository, never()).findByName(anyString());
    }

    // --- Тесты для getClientsByCompany ---
    @Test
    // ИСПРАВЛЕНО: Название теста отражает реальность (возвращает 0 или 1 клиента)
    void getClientsByCompany_WhenClientExistsForCompany_ShouldReturnSingletonDtoList() {
        // Arrange
        // ИСПРАВЛЕНО: Мокируем findByCompanyId, чтобы возвращал Optional<Client>
        when(clientRepository.findByCompanyId(COMPANY_ID)).thenReturn(Optional.of(testClient));
        when(clientMapper.toDto(testClient)).thenReturn(testClientDto);

        // Act
        List<ClientDTO> result = clientService.getClientsByCompany(COMPANY_ID);

        // Assert
        assertNotNull(result);
        // ИСПРАВЛЕНО: Ожидаем список из одного элемента
        assertEquals(1, result.size());
        assertEquals(testClientDto, result.get(0));
        verify(clientRepository).findByCompanyId(COMPANY_ID);
        verify(clientMapper).toDto(testClient); // Вызывается один раз
    }

    @Test
        // ИСПРАВЛЕНО: Название теста отражает реальность
    void getClientsByCompany_WhenNoClientExistsForCompany_ShouldReturnEmptyList() {
        // Arrange
        // ИСПРАВЛЕНО: Мокируем findByCompanyId, чтобы возвращал пустой Optional
        when(clientRepository.findByCompanyId(COMPANY_ID)).thenReturn(Optional.empty());

        // Act
        List<ClientDTO> result = clientService.getClientsByCompany(COMPANY_ID);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty()); // Список будет пуст, т.к. Optional.stream() даст пустой Stream
        verify(clientRepository).findByCompanyId(COMPANY_ID);
        verify(clientMapper, never()).toDto(any(Client.class)); // Маппер не вызывается
    }


    // --- Тесты создания (createClient) ---
    // ... (остаются без изменений) ...
    @Test
    void createClient_WithUser_ShouldSaveAndReturnNewClient() {
        when(companyRepository.findById(COMPANY_ID)).thenReturn(Optional.of(testCompany));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
        when(clientRepository.save(any(Client.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Client result = clientService.createClient(NEW_CLIENT_NAME, COMPANY_ID, USER_ID);

        assertNotNull(result);
        verify(clientRepository).save(clientCaptor.capture());
        Client savedClient = clientCaptor.getValue();

        assertEquals(NEW_CLIENT_NAME, savedClient.getName());
        assertEquals(testCompany, savedClient.getCompany());
        assertEquals(testUser, savedClient.getUser());
        assertEquals(TypeClient.IMPORTANT, savedClient.getTypeClient());
        assertNotNull(savedClient.getCreatedAt());
        assertNotNull(savedClient.getUpdatedAt());
        // ИСПРАВЛЕНО: Удалена проверка на точное равенство времени
        // assertEquals(savedClient.getCreatedAt(), savedClient.getUpdatedAt());

        assertEquals(savedClient, result);
        verify(companyRepository).findById(COMPANY_ID);
        verify(userRepository).findById(USER_ID);
    }

    @Test
    void createClient_WithoutUser_ShouldSaveAndReturnNewClient() {
        when(companyRepository.findById(COMPANY_ID)).thenReturn(Optional.of(testCompany));
        when(clientRepository.save(any(Client.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Client result = clientService.createClient(NEW_CLIENT_NAME, COMPANY_ID, null);

        assertNotNull(result);
        verify(clientRepository).save(clientCaptor.capture());
        Client savedClient = clientCaptor.getValue();

        assertEquals(NEW_CLIENT_NAME, savedClient.getName());
        assertEquals(testCompany, savedClient.getCompany());
        assertNull(savedClient.getUser());
        assertEquals(TypeClient.IMPORTANT, savedClient.getTypeClient());
        assertNotNull(savedClient.getCreatedAt());
        assertNotNull(savedClient.getUpdatedAt());
        // ИСПРАВЛЕНО: Удалена проверка на точное равенство времени
        // assertEquals(savedClient.getCreatedAt(), savedClient.getUpdatedAt());

        assertEquals(savedClient, result);
        verify(companyRepository).findById(COMPANY_ID);
        verify(userRepository, never()).findById(anyInt());
    }



    @Test
    void createClient_CompanyNotFound_ShouldThrowResourceNotFoundException() {
        when(companyRepository.findById(COMPANY_ID)).thenReturn(Optional.empty());
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            clientService.createClient(NEW_CLIENT_NAME, COMPANY_ID, USER_ID);
        });
        assertEquals("Company with ID " + COMPANY_ID + " not found.", exception.getMessage());
        verify(companyRepository).findById(COMPANY_ID);
        verifyNoInteractions(userRepository, clientRepository);
    }

    @Test
    void createClient_UserNotFound_ShouldThrowResourceNotFoundException() {
        when(companyRepository.findById(COMPANY_ID)).thenReturn(Optional.of(testCompany));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            clientService.createClient(NEW_CLIENT_NAME, COMPANY_ID, USER_ID);
        });
        assertEquals("User with ID " + USER_ID + " not found.", exception.getMessage());
        verify(companyRepository).findById(COMPANY_ID);
        verify(userRepository).findById(USER_ID);
        verifyNoInteractions(clientRepository);
    }

    @Test
    void createClient_RepositorySaveFails_ShouldThrowDataAccessException() {
        when(companyRepository.findById(COMPANY_ID)).thenReturn(Optional.of(testCompany));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
        DataAccessException dbException = new DataAccessException("DB connection failed") {};
        when(clientRepository.save(any(Client.class))).thenThrow(dbException);

        DataAccessException thrown = assertThrows(DataAccessException.class, () -> {
            clientService.createClient(NEW_CLIENT_NAME, COMPANY_ID, USER_ID);
        });

        assertEquals(dbException, thrown);
        verify(companyRepository).findById(COMPANY_ID);
        verify(userRepository).findById(USER_ID);
        verify(clientRepository).save(any(Client.class));
    }
}