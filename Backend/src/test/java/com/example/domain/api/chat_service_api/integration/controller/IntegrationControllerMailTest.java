package com.example.domain.api.chat_service_api.integration.controller;

import com.example.database.model.company_subscription_module.company.Company;
import com.example.database.model.company_subscription_module.company.CompanyMailConfiguration;
import com.example.domain.api.chat_service_api.integration.dto.IntegrationMailDto;
import com.example.domain.api.chat_service_api.integration.dto.rest.CreateMailConfigurationRequest;
import com.example.domain.api.chat_service_api.integration.mapper.IntegrationMapper;
import com.example.domain.api.chat_service_api.integration.service.IIntegrationService;
import com.example.domain.dto.CompanyDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.persistence.EntityNotFoundException;
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
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.AccessDeniedException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("IntegrationController - Mail Unit Tests")
class IntegrationControllerMailTest {

    @Mock
    private IIntegrationService integrationService;

    @Mock
    private IntegrationMapper integrationMapper;

    @InjectMocks
    private IntegrationController integrationController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private final Integer MOCK_CONFIG_ID = 2; // Другой ID для отличия
    private final Integer MOCK_COMPANY_ID = 100;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        mockMvc = MockMvcBuilders.standaloneSetup(integrationController).build();
    }

    private CompanyMailConfiguration createMockMailConfigEntity() {
        CompanyMailConfiguration entity = new CompanyMailConfiguration();
        entity.setId(MOCK_CONFIG_ID);
        entity.setEmailAddress("test@example.com");
        entity.setAppPassword("password");
        entity.setImapServer("imap.example.com");
        Company company = new Company();
        company.setId(MOCK_COMPANY_ID);
        company.setName("Test Company");
        entity.setCompany(company);
        entity.setCreatedAt(LocalDateTime.now());
        return entity;
    }

    private IntegrationMailDto createMockIntegrationMailDto() {
        IntegrationMailDto dto = new IntegrationMailDto();
        dto.setId(MOCK_CONFIG_ID);
        dto.setEmailAddress("test@example.com");
        dto.setAppPassword("password"); // В DTO обычно не включают пароли, но если он есть...
        dto.setImapServer("imap.example.com");
        CompanyDto companyDto = CompanyDto.builder().id(MOCK_COMPANY_ID).name("Test Company").build();
        dto.setCompanyDto(companyDto);
        dto.setCreatedAt(LocalDateTime.now());
        return dto;
    }

    @Nested
    @DisplayName("POST /api/ui/integration/mail")
    class CreateOrUpdateMailIntegration {
        @Test
        @DisplayName("should return IntegrationMailDto and OK on successful creation/update")
        void createOrUpdate_successful() throws Exception {
            CreateMailConfigurationRequest request = CreateMailConfigurationRequest.builder()
                    .emailAddress("new@example.com")
                    .appPassword("newpass")
                    .build();

            CompanyMailConfiguration configEntity = createMockMailConfigEntity();
            IntegrationMailDto responseDto = createMockIntegrationMailDto();

            when(integrationService.createCompanyMailConfiguration(any(CreateMailConfigurationRequest.class)))
                    .thenReturn(configEntity);
            when(integrationMapper.toMailDto(configEntity)).thenReturn(responseDto);

            mockMvc.perform(post("/api/ui/integration/mail")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(responseDto.getId())))
                    .andExpect(jsonPath("$.emailAddress", is(responseDto.getEmailAddress())));

            verify(integrationService).createCompanyMailConfiguration(any(CreateMailConfigurationRequest.class));
            verify(integrationMapper).toMailDto(configEntity);
        }

        @Test
        @DisplayName("should return FORBIDDEN when service throws AccessDeniedException")
        void createOrUpdate_accessDenied() throws Exception {
            CreateMailConfigurationRequest request = CreateMailConfigurationRequest.builder().build();
            when(integrationService.createCompanyMailConfiguration(any(CreateMailConfigurationRequest.class)))
                    .thenThrow(new AccessDeniedException("Access Denied to create Mail config"));

            mockMvc.perform(post("/api/ui/integration/mail")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden())
                    .andExpect(result -> assertTrue(result.getResolvedException() instanceof ResponseStatusException))
                    .andExpect(result -> assertEquals("Access Denied", ((ResponseStatusException)result.getResolvedException()).getReason()));
        }

        @Test
        @DisplayName("should return BAD_REQUEST when service throws other Exception")
        void createOrUpdate_otherException() throws Exception {
            CreateMailConfigurationRequest request = CreateMailConfigurationRequest.builder().build();
            String errorMessage = "Invalid mail server config";
            when(integrationService.createCompanyMailConfiguration(any(CreateMailConfigurationRequest.class)))
                    .thenThrow(new IllegalArgumentException(errorMessage));

            mockMvc.perform(post("/api/ui/integration/mail")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(result -> assertTrue(result.getResolvedException() instanceof ResponseStatusException))
                    .andExpect(result -> assertEquals(errorMessage, ((ResponseStatusException)result.getResolvedException()).getReason()));
        }
    }

    @Nested
    @DisplayName("GET /api/ui/integration/mail")
    class GetAllMailIntegrations {
        @Test
        @DisplayName("should return list of IntegrationMailDto and OK")
        void getAll_successful() throws Exception {
            List<CompanyMailConfiguration> configEntities = List.of(createMockMailConfigEntity());
            List<IntegrationMailDto> dtoList = List.of(createMockIntegrationMailDto());

            when(integrationService.getAllMailConfigurations()).thenReturn(configEntities);
            // В контроллере используется integrationMapper.toMailDtoList(configs);
            when(integrationMapper.toMailDtoList(configEntities)).thenReturn(dtoList);

            mockMvc.perform(get("/api/ui/integration/mail")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id", is(dtoList.get(0).getId())));

            verify(integrationService).getAllMailConfigurations();
            verify(integrationMapper).toMailDtoList(configEntities);
        }

        @Test
        @DisplayName("should return FORBIDDEN when service throws AccessDeniedException")
        void getAll_accessDenied() throws Exception {
            when(integrationService.getAllMailConfigurations())
                    .thenThrow(new AccessDeniedException("Access Denied to list Mail configs"));

            mockMvc.perform(get("/api/ui/integration/mail")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /api/ui/integration/mail/{id}")
    class GetMailIntegrationById {
        @Test
        @DisplayName("should return IntegrationMailDto and OK for existing ID")
        void getById_successful() throws Exception {
            CompanyMailConfiguration configEntity = createMockMailConfigEntity();
            IntegrationMailDto responseDto = createMockIntegrationMailDto();

            when(integrationService.getMailConfigurationById(MOCK_CONFIG_ID)).thenReturn(configEntity);
            when(integrationMapper.toMailDto(configEntity)).thenReturn(responseDto);

            mockMvc.perform(get("/api/ui/integration/mail/{id}", MOCK_CONFIG_ID)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(responseDto.getId())));

            verify(integrationService).getMailConfigurationById(MOCK_CONFIG_ID);
            verify(integrationMapper).toMailDto(configEntity);
        }

        @Test
        @DisplayName("should return NOT_FOUND when service throws RuntimeException (e.g., EntityNotFound)")
        void getById_notFound() throws Exception {
            String errorMessage = "Mail config not found";
            when(integrationService.getMailConfigurationById(MOCK_CONFIG_ID))
                    .thenThrow(new EntityNotFoundException(errorMessage));

            mockMvc.perform(get("/api/ui/integration/mail/{id}", MOCK_CONFIG_ID)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(result -> assertTrue(result.getResolvedException() instanceof ResponseStatusException))
                    .andExpect(result -> assertEquals(errorMessage, ((ResponseStatusException)result.getResolvedException()).getReason()));
        }

        @Test
        @DisplayName("should return FORBIDDEN when service throws AccessDeniedException")
        void getById_accessDenied() throws Exception {
            when(integrationService.getMailConfigurationById(MOCK_CONFIG_ID))
                    .thenThrow(new AccessDeniedException("Access Denied"));

            mockMvc.perform(get("/api/ui/integration/mail/{id}", MOCK_CONFIG_ID)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("DELETE /api/ui/integration/mail/{id}")
    class DeleteMailIntegration {
        @Test
        @DisplayName("should return NO_CONTENT on successful deletion")
        void delete_successful() throws Exception {
            doNothing().when(integrationService).deleteMailConfiguration(MOCK_CONFIG_ID);

            mockMvc.perform(delete("/api/ui/integration/mail/{id}", MOCK_CONFIG_ID))
                    .andExpect(status().isNoContent());

            verify(integrationService).deleteMailConfiguration(MOCK_CONFIG_ID);
        }

        @Test
        @DisplayName("should return NOT_FOUND when service throws RuntimeException (e.g., EntityNotFound)")
        void delete_notFound() throws Exception {
            String errorMessage = "Mail config not found for deletion";
            doThrow(new EntityNotFoundException(errorMessage)).when(integrationService).deleteMailConfiguration(MOCK_CONFIG_ID);

            mockMvc.perform(delete("/api/ui/integration/mail/{id}", MOCK_CONFIG_ID))
                    .andExpect(status().isNotFound())
                    .andExpect(result -> assertTrue(result.getResolvedException() instanceof ResponseStatusException))
                    .andExpect(result -> assertEquals(errorMessage, ((ResponseStatusException)result.getResolvedException()).getReason()));
        }

        @Test
        @DisplayName("should return FORBIDDEN when service throws AccessDeniedException")
        void delete_accessDenied() throws Exception {
            doThrow(new AccessDeniedException("Access Denied")).when(integrationService).deleteMailConfiguration(MOCK_CONFIG_ID);

            mockMvc.perform(delete("/api/ui/integration/mail/{id}", MOCK_CONFIG_ID))
                    .andExpect(status().isForbidden());
        }
    }
}