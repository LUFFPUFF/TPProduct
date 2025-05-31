package com.example.domain.api.chat_service_api.integration.controller;

import com.example.database.model.company_subscription_module.company.Company;
import com.example.database.model.company_subscription_module.company.CompanyWhatsappConfiguration;
import com.example.domain.api.chat_service_api.integration.dto.IntegrationWhatsappDto;
import com.example.domain.api.chat_service_api.integration.dto.rest.CreateWhatsappConfigurationRequest;
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
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("IntegrationController - WhatsApp Unit Tests")
class IntegrationControllerWhatsappTest {

    @Mock
    private IIntegrationService integrationService;

    @Mock
    private IntegrationMapper integrationMapper;

    @InjectMocks
    private IntegrationController integrationController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private final Integer MOCK_CONFIG_ID = 3; // Уникальный ID
    private final Integer MOCK_COMPANY_ID = 100;
    private final Long MOCK_PHONE_NUMBER_ID = 1234567890L;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        mockMvc = MockMvcBuilders.standaloneSetup(integrationController).build();
    }

    private CompanyWhatsappConfiguration createMockWhatsappConfigEntity() {
        CompanyWhatsappConfiguration entity = new CompanyWhatsappConfiguration();
        entity.setId(MOCK_CONFIG_ID);
        entity.setPhoneNumberId(MOCK_PHONE_NUMBER_ID);
        entity.setAccessToken("wa-access-token");
        entity.setVerifyToken("wa-verify-token");
        Company company = new Company();
        company.setId(MOCK_COMPANY_ID);
        company.setName("Test Company");
        entity.setCompany(company);
        entity.setCreatedAt(LocalDateTime.now());
        return entity;
    }

    private IntegrationWhatsappDto createMockIntegrationWhatsappDto() {
        IntegrationWhatsappDto dto = new IntegrationWhatsappDto();
        dto.setId(MOCK_CONFIG_ID);
        // В IntegrationWhatsappDto нет phoneNumberId и accessToken, только verifyToken
        dto.setVerifyToken("wa-verify-token");
        CompanyDto companyDto = CompanyDto.builder().id(MOCK_COMPANY_ID).name("Test Company").build();
        dto.setCompanyDto(companyDto);
        dto.setCreatedAt(LocalDateTime.now());
        return dto;
    }

    @Nested
    @DisplayName("POST /api/ui/integration/whatsapp")
    class CreateOrUpdateWhatsappIntegration {
        @Test
        @DisplayName("should return IntegrationWhatsappDto and OK on successful creation/update")
        void createOrUpdate_successful() throws Exception {
            CreateWhatsappConfigurationRequest request = new CreateWhatsappConfigurationRequest();
            request.setPhoneNumberId(MOCK_PHONE_NUMBER_ID);
            request.setAccessToken("new-wa-access-token");
            request.setVerifyToken("new-wa-verify-token");

            CompanyWhatsappConfiguration configEntity = createMockWhatsappConfigEntity();
            IntegrationWhatsappDto responseDto = createMockIntegrationWhatsappDto();

            when(integrationService.createCompanyWhatsappConfiguration(any(CreateWhatsappConfigurationRequest.class)))
                    .thenReturn(configEntity);
            when(integrationMapper.toWhatsappDto(configEntity)).thenReturn(responseDto);

            mockMvc.perform(post("/api/ui/integration/whatsapp")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(responseDto.getId())))
                    .andExpect(jsonPath("$.verifyToken", is(responseDto.getVerifyToken())));

            verify(integrationService).createCompanyWhatsappConfiguration(any(CreateWhatsappConfigurationRequest.class));
            verify(integrationMapper).toWhatsappDto(configEntity);
        }

        @Test
        @DisplayName("should return FORBIDDEN when service throws AccessDeniedException")
        void createOrUpdate_accessDenied() throws Exception {
            CreateWhatsappConfigurationRequest request = new CreateWhatsappConfigurationRequest();
            when(integrationService.createCompanyWhatsappConfiguration(any(CreateWhatsappConfigurationRequest.class)))
                    .thenThrow(new AccessDeniedException("Access Denied to create WhatsApp config"));

            mockMvc.perform(post("/api/ui/integration/whatsapp")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden())
                    .andExpect(result -> assertTrue(result.getResolvedException() instanceof ResponseStatusException))
                    .andExpect(result -> assertEquals("Access Denied", ((ResponseStatusException)result.getResolvedException()).getReason()));
        }

        @Test
        @DisplayName("should return BAD_REQUEST when service throws other Exception")
        void createOrUpdate_otherException() throws Exception {
            CreateWhatsappConfigurationRequest request = new CreateWhatsappConfigurationRequest();
            String errorMessage = "Invalid WhatsApp API credentials";
            when(integrationService.createCompanyWhatsappConfiguration(any(CreateWhatsappConfigurationRequest.class)))
                    .thenThrow(new IllegalArgumentException(errorMessage));

            mockMvc.perform(post("/api/ui/integration/whatsapp")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(result -> assertTrue(result.getResolvedException() instanceof ResponseStatusException))
                    .andExpect(result -> assertEquals(errorMessage, ((ResponseStatusException)result.getResolvedException()).getReason()));
        }
    }

    @Nested
    @DisplayName("GET /api/ui/integration/whatsapp")
    class GetAllWhatsappIntegrations {
        @Test
        @DisplayName("should return list of IntegrationWhatsappDto and OK")
        void getAll_successful() throws Exception {
            List<CompanyWhatsappConfiguration> configEntities = List.of(createMockWhatsappConfigEntity());
            IntegrationWhatsappDto whatsappDto = createMockIntegrationWhatsappDto();

            when(integrationService.getAllWhatsappConfigurations()).thenReturn(configEntities);
            // Контроллер использует stream().map(integrationMapper::toWhatsappDto)
            when(integrationMapper.toWhatsappDto(any(CompanyWhatsappConfiguration.class))).thenReturn(whatsappDto);


            mockMvc.perform(get("/api/ui/integration/whatsapp")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id", is(whatsappDto.getId())));

            verify(integrationService).getAllWhatsappConfigurations();
            verify(integrationMapper, times(configEntities.size())).toWhatsappDto(any(CompanyWhatsappConfiguration.class));
        }

        @Test
        @DisplayName("should return FORBIDDEN when service throws AccessDeniedException")
        void getAll_accessDenied() throws Exception {
            when(integrationService.getAllWhatsappConfigurations())
                    .thenThrow(new AccessDeniedException("Access Denied to list WhatsApp configs"));

            mockMvc.perform(get("/api/ui/integration/whatsapp")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /api/ui/integration/whatsapp/{id}")
    class GetWhatsappIntegrationById {
        @Test
        @DisplayName("should return IntegrationWhatsappDto and OK for existing ID")
        void getById_successful() throws Exception {
            CompanyWhatsappConfiguration configEntity = createMockWhatsappConfigEntity();
            IntegrationWhatsappDto responseDto = createMockIntegrationWhatsappDto();

            when(integrationService.getWhatsappConfigurationById(MOCK_CONFIG_ID)).thenReturn(configEntity);
            when(integrationMapper.toWhatsappDto(configEntity)).thenReturn(responseDto);

            mockMvc.perform(get("/api/ui/integration/whatsapp/{id}", MOCK_CONFIG_ID)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(responseDto.getId())));

            verify(integrationService).getWhatsappConfigurationById(MOCK_CONFIG_ID);
            verify(integrationMapper).toWhatsappDto(configEntity);
        }

        @Test
        @DisplayName("should return NOT_FOUND when service throws RuntimeException (e.g., EntityNotFound)")
        void getById_notFound() throws Exception {
            String errorMessage = "WhatsApp config not found";
            when(integrationService.getWhatsappConfigurationById(MOCK_CONFIG_ID))
                    .thenThrow(new EntityNotFoundException(errorMessage));

            mockMvc.perform(get("/api/ui/integration/whatsapp/{id}", MOCK_CONFIG_ID)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(result -> assertTrue(result.getResolvedException() instanceof ResponseStatusException))
                    .andExpect(result -> assertEquals(errorMessage, ((ResponseStatusException)result.getResolvedException()).getReason()));
        }

        @Test
        @DisplayName("should return FORBIDDEN when service throws AccessDeniedException")
        void getById_accessDenied() throws Exception {
            when(integrationService.getWhatsappConfigurationById(MOCK_CONFIG_ID))
                    .thenThrow(new AccessDeniedException("Access Denied"));

            mockMvc.perform(get("/api/ui/integration/whatsapp/{id}", MOCK_CONFIG_ID)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("DELETE /api/ui/integration/whatsapp/{id}")
    class DeleteWhatsappIntegration {
        @Test
        @DisplayName("should return NO_CONTENT on successful deletion")
        void delete_successful() throws Exception {
            doNothing().when(integrationService).deleteWhatsappConfiguration(MOCK_CONFIG_ID);

            mockMvc.perform(delete("/api/ui/integration/whatsapp/{id}", MOCK_CONFIG_ID))
                    .andExpect(status().isNoContent());

            verify(integrationService).deleteWhatsappConfiguration(MOCK_CONFIG_ID);
        }

        @Test
        @DisplayName("should return NOT_FOUND when service throws RuntimeException (e.g., EntityNotFound)")
        void delete_notFound() throws Exception {
            String errorMessage = "WhatsApp config not found for deletion";
            doThrow(new EntityNotFoundException(errorMessage)).when(integrationService).deleteWhatsappConfiguration(MOCK_CONFIG_ID);

            mockMvc.perform(delete("/api/ui/integration/whatsapp/{id}", MOCK_CONFIG_ID))
                    .andExpect(status().isNotFound())
                    .andExpect(result -> assertTrue(result.getResolvedException() instanceof ResponseStatusException))
                    .andExpect(result -> assertEquals(errorMessage, ((ResponseStatusException)result.getResolvedException()).getReason()));
        }

        @Test
        @DisplayName("should return FORBIDDEN when service throws AccessDeniedException")
        void delete_accessDenied() throws Exception {
            doThrow(new AccessDeniedException("Access Denied")).when(integrationService).deleteWhatsappConfiguration(MOCK_CONFIG_ID);

            mockMvc.perform(delete("/api/ui/integration/whatsapp/{id}", MOCK_CONFIG_ID))
                    .andExpect(status().isForbidden());
        }
    }
}