package com.example.domain.api.chat_service_api.integration.controller;

import com.example.database.model.company_subscription_module.company.Company;
import com.example.database.model.company_subscription_module.company.CompanyVkConfiguration;
import com.example.domain.api.chat_service_api.integration.dto.IntegrationVkDto;
import com.example.domain.api.chat_service_api.integration.dto.rest.CreateVkConfigurationRequest;
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
@DisplayName("IntegrationController - VK Unit Tests")
class IntegrationControllerVkTest {

    @Mock
    private IIntegrationService integrationService;

    @Mock
    private IntegrationMapper integrationMapper;

    @InjectMocks
    private IntegrationController integrationController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private final Integer MOCK_CONFIG_ID = 4; // Уникальный ID
    private final Integer MOCK_COMPANY_ID = 100;
    private final Long MOCK_COMMUNITY_ID = 987654321L;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        mockMvc = MockMvcBuilders.standaloneSetup(integrationController).build();
    }

    private CompanyVkConfiguration createMockVkConfigEntity() {
        CompanyVkConfiguration entity = new CompanyVkConfiguration();
        entity.setId(MOCK_CONFIG_ID);
        entity.setCommunityId(MOCK_COMMUNITY_ID);
        entity.setAccessToken("vk-access-token");
        entity.setCommunityName("VK Test Community");
        Company company = new Company();
        company.setId(MOCK_COMPANY_ID);
        company.setName("Test Company");
        entity.setCompany(company);
        entity.setCreatedAt(LocalDateTime.now());
        // entity.setActive(true); // isActive() вычисляется
        return entity;
    }

    private IntegrationVkDto createMockIntegrationVkDto() {
        IntegrationVkDto dto = new IntegrationVkDto();
        dto.setId(MOCK_CONFIG_ID);
        dto.setCommunityId(MOCK_COMMUNITY_ID);
        dto.setCommunityName("VK Test Community");
        CompanyDto companyDto = CompanyDto.builder().id(MOCK_COMPANY_ID).name("Test Company").build();
        dto.setCompanyDto(companyDto);
        dto.setCreatedAt(LocalDateTime.now());
        dto.setActive(true); // Предполагаем, что мок сущности будет активным
        return dto;
    }

    @Nested
    @DisplayName("POST /api/ui/integration/vk")
    class CreateOrUpdateVkIntegration {
        @Test
        @DisplayName("should return IntegrationVkDto and OK on successful creation/update")
        void createOrUpdate_successful() throws Exception {
            CreateVkConfigurationRequest request = new CreateVkConfigurationRequest();
            request.setCommunityId(MOCK_COMMUNITY_ID);
            request.setAccessToken("new-vk-access-token");
            request.setCommunityName("New VK Community");

            CompanyVkConfiguration configEntity = createMockVkConfigEntity();
            IntegrationVkDto responseDto = createMockIntegrationVkDto();
            // Убедимся, что мок ответа соответствует запросу
            responseDto.setCommunityName(request.getCommunityName());

            when(integrationService.createCompanyVkConfiguration(any(CreateVkConfigurationRequest.class)))
                    .thenReturn(configEntity);
            when(integrationMapper.toVkDto(configEntity)).thenReturn(responseDto);

            mockMvc.perform(post("/api/ui/integration/vk")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(responseDto.getId())))
                    .andExpect(jsonPath("$.communityName", is(responseDto.getCommunityName())));

            verify(integrationService).createCompanyVkConfiguration(any(CreateVkConfigurationRequest.class));
            verify(integrationMapper).toVkDto(configEntity);
        }

        @Test
        @DisplayName("should return FORBIDDEN when service throws AccessDeniedException")
        void createOrUpdate_accessDenied() throws Exception {
            CreateVkConfigurationRequest request = new CreateVkConfigurationRequest();
            when(integrationService.createCompanyVkConfiguration(any(CreateVkConfigurationRequest.class)))
                    .thenThrow(new AccessDeniedException("Access Denied to create VK config"));

            mockMvc.perform(post("/api/ui/integration/vk")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden())
                    .andExpect(result -> assertTrue(result.getResolvedException() instanceof ResponseStatusException))
                    .andExpect(result -> assertEquals("Access Denied", ((ResponseStatusException)result.getResolvedException()).getReason()));
        }

        @Test
        @DisplayName("should return BAD_REQUEST when service throws other Exception")
        void createOrUpdate_otherException() throws Exception {
            CreateVkConfigurationRequest request = new CreateVkConfigurationRequest();
            String errorMessage = "Invalid VK API credentials";
            when(integrationService.createCompanyVkConfiguration(any(CreateVkConfigurationRequest.class)))
                    .thenThrow(new IllegalArgumentException(errorMessage));

            mockMvc.perform(post("/api/ui/integration/vk")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(result -> assertTrue(result.getResolvedException() instanceof ResponseStatusException))
                    .andExpect(result -> assertEquals(errorMessage, ((ResponseStatusException)result.getResolvedException()).getReason()));
        }
    }

    @Nested
    @DisplayName("GET /api/ui/integration/vk")
    class GetAllVkIntegrations {
        @Test
        @DisplayName("should return list of IntegrationVkDto and OK")
        void getAll_successful() throws Exception {
            List<CompanyVkConfiguration> configEntities = List.of(createMockVkConfigEntity());
            IntegrationVkDto vkDto = createMockIntegrationVkDto();

            when(integrationService.getAllVkConfigurations()).thenReturn(configEntities);
            // Контроллер использует stream().map(integrationMapper::toVkDto)
            when(integrationMapper.toVkDto(any(CompanyVkConfiguration.class))).thenReturn(vkDto);


            mockMvc.perform(get("/api/ui/integration/vk")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id", is(vkDto.getId())));

            verify(integrationService).getAllVkConfigurations();
            verify(integrationMapper, times(configEntities.size())).toVkDto(any(CompanyVkConfiguration.class));
        }

        @Test
        @DisplayName("should return FORBIDDEN when service throws AccessDeniedException")
        void getAll_accessDenied() throws Exception {
            when(integrationService.getAllVkConfigurations())
                    .thenThrow(new AccessDeniedException("Access Denied to list VK configs"));

            mockMvc.perform(get("/api/ui/integration/vk")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /api/ui/integration/vk/{id}")
    class GetVkIntegrationById {
        @Test
        @DisplayName("should return IntegrationVkDto and OK for existing ID")
        void getById_successful() throws Exception {
            CompanyVkConfiguration configEntity = createMockVkConfigEntity();
            IntegrationVkDto responseDto = createMockIntegrationVkDto();

            when(integrationService.getVkConfigurationById(MOCK_CONFIG_ID)).thenReturn(configEntity);
            when(integrationMapper.toVkDto(configEntity)).thenReturn(responseDto);

            mockMvc.perform(get("/api/ui/integration/vk/{id}", MOCK_CONFIG_ID)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(responseDto.getId())));

            verify(integrationService).getVkConfigurationById(MOCK_CONFIG_ID);
            verify(integrationMapper).toVkDto(configEntity);
        }

        @Test
        @DisplayName("should return NOT_FOUND when service throws RuntimeException (e.g., EntityNotFound)")
        void getById_notFound() throws Exception {
            String errorMessage = "VK config not found";
            when(integrationService.getVkConfigurationById(MOCK_CONFIG_ID))
                    .thenThrow(new EntityNotFoundException(errorMessage));

            mockMvc.perform(get("/api/ui/integration/vk/{id}", MOCK_CONFIG_ID)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(result -> assertTrue(result.getResolvedException() instanceof ResponseStatusException))
                    .andExpect(result -> assertEquals(errorMessage, ((ResponseStatusException)result.getResolvedException()).getReason()));
        }

        @Test
        @DisplayName("should return FORBIDDEN when service throws AccessDeniedException")
        void getById_accessDenied() throws Exception {
            when(integrationService.getVkConfigurationById(MOCK_CONFIG_ID))
                    .thenThrow(new AccessDeniedException("Access Denied"));

            mockMvc.perform(get("/api/ui/integration/vk/{id}", MOCK_CONFIG_ID)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("DELETE /api/ui/integration/vk/{id}")
    class DeleteVkIntegration {
        @Test
        @DisplayName("should return NO_CONTENT on successful deletion")
        void delete_successful() throws Exception {
            doNothing().when(integrationService).deleteVkConfiguration(MOCK_CONFIG_ID);

            mockMvc.perform(delete("/api/ui/integration/vk/{id}", MOCK_CONFIG_ID))
                    .andExpect(status().isNoContent());

            verify(integrationService).deleteVkConfiguration(MOCK_CONFIG_ID);
        }

        @Test
        @DisplayName("should return NOT_FOUND when service throws RuntimeException (e.g., EntityNotFound)")
        void delete_notFound() throws Exception {
            String errorMessage = "VK config not found for deletion";
            doThrow(new EntityNotFoundException(errorMessage)).when(integrationService).deleteVkConfiguration(MOCK_CONFIG_ID);

            mockMvc.perform(delete("/api/ui/integration/vk/{id}", MOCK_CONFIG_ID))
                    .andExpect(status().isNotFound())
                    .andExpect(result -> assertTrue(result.getResolvedException() instanceof ResponseStatusException))
                    .andExpect(result -> assertEquals(errorMessage, ((ResponseStatusException)result.getResolvedException()).getReason()));
        }

        @Test
        @DisplayName("should return FORBIDDEN when service throws AccessDeniedException")
        void delete_accessDenied() throws Exception {
            doThrow(new AccessDeniedException("Access Denied")).when(integrationService).deleteVkConfiguration(MOCK_CONFIG_ID);

            mockMvc.perform(delete("/api/ui/integration/vk/{id}", MOCK_CONFIG_ID))
                    .andExpect(status().isForbidden());
        }
    }
}