package com.example.domain.api.chat_service_api.integration.controller;

import com.example.database.model.company_subscription_module.company.Company;
import com.example.database.model.company_subscription_module.company.CompanyTelegramConfiguration;
import com.example.domain.api.chat_service_api.integration.dto.IntegrationTelegramDto;
import com.example.domain.api.chat_service_api.integration.dto.rest.CreateTelegramConfigurationRequest;
import com.example.domain.api.chat_service_api.integration.mapper.IntegrationMapper;
import com.example.domain.api.chat_service_api.integration.service.IIntegrationService;
import com.example.domain.dto.CompanyDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.persistence.EntityNotFoundException; // Стандартное исключение JPA
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException; // Для проверки типа исключения

import java.nio.file.AccessDeniedException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("IntegrationController - Telegram Unit Tests")
class IntegrationControllerTelegramTest { // Изменил имя класса для фокуса на Telegram

    @Mock
    private IIntegrationService integrationService;

    @Mock
    private IntegrationMapper integrationMapper;

    @InjectMocks
    private IntegrationController integrationController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private final Integer MOCK_CONFIG_ID = 1;
    private final Integer MOCK_COMPANY_ID = 100;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        // Настройка MockMvc с обработчиком ResponseStatusException по умолчанию
        mockMvc = MockMvcBuilders.standaloneSetup(integrationController)
                // .setControllerAdvice(new YourGlobalExceptionHandlerIfAny()) // Если есть кастомный обработчик
                .build();
    }

    private CompanyTelegramConfiguration createMockTelegramConfigEntity() {
        CompanyTelegramConfiguration entity = new CompanyTelegramConfiguration();
        entity.setId(MOCK_CONFIG_ID);
        entity.setBotToken("test-token");
        entity.setBotUsername("test_bot");
        entity.setChatTelegramId(12345L);
        Company company = new Company();
        company.setId(MOCK_COMPANY_ID);
        company.setName("Test Company");
        entity.setCompany(company);
        entity.setCreatedAt(LocalDateTime.now());
        return entity;
    }

    private IntegrationTelegramDto createMockIntegrationTelegramDto() {
        IntegrationTelegramDto dto = new IntegrationTelegramDto();
        dto.setId(MOCK_CONFIG_ID);
        dto.setBotToken("test-token");
        dto.setBotUsername("test_bot");
        dto.setChatTelegramId(12345L);
        CompanyDto companyDto = CompanyDto.builder().id(MOCK_COMPANY_ID).name("Test Company").build();
        dto.setCompanyDto(companyDto);
        dto.setCreatedAt(LocalDateTime.now());
        return dto;
    }

    @Nested
    @DisplayName("POST /api/ui/integration/telegram")
    class CreateOrUpdateTelegramIntegration {
        @Test
        @DisplayName("should return IntegrationTelegramDto and OK on successful creation/update")
        void createOrUpdate_successful() throws Exception {
            CreateTelegramConfigurationRequest request = new CreateTelegramConfigurationRequest();
            request.setBotToken("new-token");
            request.setBotUsername("new_bot");

            CompanyTelegramConfiguration configEntity = createMockTelegramConfigEntity();
            IntegrationTelegramDto responseDto = createMockIntegrationTelegramDto();

            when(integrationService.createCompanyTelegramConfiguration(any(CreateTelegramConfigurationRequest.class)))
                    .thenReturn(configEntity);
            when(integrationMapper.toTelegramDto(configEntity)).thenReturn(responseDto);

            mockMvc.perform(post("/api/ui/integration/telegram")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(responseDto.getId())))
                    .andExpect(jsonPath("$.botToken", is(responseDto.getBotToken())));

            verify(integrationService).createCompanyTelegramConfiguration(any(CreateTelegramConfigurationRequest.class));
            verify(integrationMapper).toTelegramDto(configEntity);
        }

        @Test
        @DisplayName("should return FORBIDDEN when service throws AccessDeniedException")
        void createOrUpdate_accessDenied() throws Exception {
            CreateTelegramConfigurationRequest request = new CreateTelegramConfigurationRequest();
            when(integrationService.createCompanyTelegramConfiguration(any(CreateTelegramConfigurationRequest.class)))
                    .thenThrow(new AccessDeniedException("Access Denied to create Telegram config"));

            mockMvc.perform(post("/api/ui/integration/telegram")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden())
                    .andExpect(result -> assertTrue(result.getResolvedException() instanceof ResponseStatusException))
                    .andExpect(result -> assertEquals("Access Denied", ((ResponseStatusException)result.getResolvedException()).getReason()));
        }

        @Test
        @DisplayName("should return BAD_REQUEST when service throws other Exception")
        void createOrUpdate_otherException() throws Exception {
            CreateTelegramConfigurationRequest request = new CreateTelegramConfigurationRequest();
            String errorMessage = "Invalid bot token format";
            when(integrationService.createCompanyTelegramConfiguration(any(CreateTelegramConfigurationRequest.class)))
                    .thenThrow(new IllegalArgumentException(errorMessage));

            mockMvc.perform(post("/api/ui/integration/telegram")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(result -> assertTrue(result.getResolvedException() instanceof ResponseStatusException))
                    .andExpect(result -> assertEquals(errorMessage, ((ResponseStatusException)result.getResolvedException()).getReason()));
        }
    }

    @Nested
    @DisplayName("GET /api/ui/integration/telegram")
    class GetAllTelegramIntegrations {
        @Test
        @DisplayName("should return list of IntegrationTelegramDto and OK")
        void getAll_successful() throws Exception {
            List<CompanyTelegramConfiguration> configEntities = List.of(createMockTelegramConfigEntity());
            IntegrationTelegramDto telegramDto = createMockIntegrationTelegramDto();
            // Мокируем маппинг для каждого элемента, т.к. используется stream().map()
            when(integrationMapper.toTelegramDto(any(CompanyTelegramConfiguration.class))).thenReturn(telegramDto);


            when(integrationService.getAllTelegramConfigurations()).thenReturn(configEntities);
            // integrationMapper.toTelegramDtoList не используется в контроллере для этого эндпоинта

            mockMvc.perform(get("/api/ui/integration/telegram")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id", is(telegramDto.getId())));

            verify(integrationService).getAllTelegramConfigurations();
            verify(integrationMapper, times(configEntities.size())).toTelegramDto(any(CompanyTelegramConfiguration.class));
        }

        @Test
        @DisplayName("should return FORBIDDEN when service throws AccessDeniedException")
        void getAll_accessDenied() throws Exception {
            when(integrationService.getAllTelegramConfigurations())
                    .thenThrow(new AccessDeniedException("Access Denied to list Telegram configs"));

            mockMvc.perform(get("/api/ui/integration/telegram")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /api/ui/integration/telegram/{id}")
    class GetTelegramIntegrationById {
        @Test
        @DisplayName("should return IntegrationTelegramDto and OK for existing ID")
        void getById_successful() throws Exception {
            CompanyTelegramConfiguration configEntity = createMockTelegramConfigEntity();
            IntegrationTelegramDto responseDto = createMockIntegrationTelegramDto();

            when(integrationService.getTelegramConfigurationById(MOCK_CONFIG_ID)).thenReturn(configEntity);
            when(integrationMapper.toTelegramDto(configEntity)).thenReturn(responseDto);

            mockMvc.perform(get("/api/ui/integration/telegram/{id}", MOCK_CONFIG_ID)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(responseDto.getId())));

            verify(integrationService).getTelegramConfigurationById(MOCK_CONFIG_ID);
            verify(integrationMapper).toTelegramDto(configEntity);
        }

        @Test
        @DisplayName("should return NOT_FOUND when service throws RuntimeException (e.g., EntityNotFound)")
        void getById_notFound() throws Exception {
            String errorMessage = "Telegram config not found";
            when(integrationService.getTelegramConfigurationById(MOCK_CONFIG_ID))
                    .thenThrow(new EntityNotFoundException(errorMessage));

            mockMvc.perform(get("/api/ui/integration/telegram/{id}", MOCK_CONFIG_ID)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(result -> assertTrue(result.getResolvedException() instanceof ResponseStatusException))
                    .andExpect(result -> assertEquals(errorMessage, ((ResponseStatusException)result.getResolvedException()).getReason()));
        }

        @Test
        @DisplayName("should return FORBIDDEN when service throws AccessDeniedException")
        void getById_accessDenied() throws Exception {
            when(integrationService.getTelegramConfigurationById(MOCK_CONFIG_ID))
                    .thenThrow(new AccessDeniedException("Access Denied"));

            mockMvc.perform(get("/api/ui/integration/telegram/{id}", MOCK_CONFIG_ID)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("DELETE /api/ui/integration/telegram/{id}")
    class DeleteTelegramIntegration {
        @Test
        @DisplayName("should return NO_CONTENT on successful deletion")
        void delete_successful() throws Exception {
            doNothing().when(integrationService).deleteTelegramConfiguration(MOCK_CONFIG_ID);

            mockMvc.perform(delete("/api/ui/integration/telegram/{id}", MOCK_CONFIG_ID))
                    .andExpect(status().isNoContent());

            verify(integrationService).deleteTelegramConfiguration(MOCK_CONFIG_ID);
        }

        @Test
        @DisplayName("should return NOT_FOUND when service throws RuntimeException (e.g., EntityNotFound)")
        void delete_notFound() throws Exception {
            String errorMessage = "Telegram config not found for deletion";
            doThrow(new EntityNotFoundException(errorMessage)).when(integrationService).deleteTelegramConfiguration(MOCK_CONFIG_ID);

            mockMvc.perform(delete("/api/ui/integration/telegram/{id}", MOCK_CONFIG_ID))
                    .andExpect(status().isNotFound())
                    .andExpect(result -> assertTrue(result.getResolvedException() instanceof ResponseStatusException))
                    .andExpect(result -> assertEquals(errorMessage, ((ResponseStatusException)result.getResolvedException()).getReason()));
        }

        @Test
        @DisplayName("should return FORBIDDEN when service throws AccessDeniedException")
        void delete_accessDenied() throws Exception {
            doThrow(new AccessDeniedException("Access Denied")).when(integrationService).deleteTelegramConfiguration(MOCK_CONFIG_ID);

            mockMvc.perform(delete("/api/ui/integration/telegram/{id}", MOCK_CONFIG_ID))
                    .andExpect(status().isForbidden());
        }
    }
}