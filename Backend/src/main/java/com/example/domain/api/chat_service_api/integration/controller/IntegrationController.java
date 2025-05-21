package com.example.domain.api.chat_service_api.integration.controller;

import com.example.database.model.company_subscription_module.company.CompanyMailConfiguration;
import com.example.database.model.company_subscription_module.company.CompanyTelegramConfiguration;
import com.example.database.model.company_subscription_module.company.CompanyVkConfiguration;
import com.example.database.model.company_subscription_module.company.CompanyWhatsappConfiguration;
import com.example.domain.api.chat_service_api.integration.dto.IntegrationMailDto;
import com.example.domain.api.chat_service_api.integration.dto.IntegrationTelegramDto;
import com.example.domain.api.chat_service_api.integration.dto.IntegrationVkDto;
import com.example.domain.api.chat_service_api.integration.dto.IntegrationWhatsappDto;
import com.example.domain.api.chat_service_api.integration.dto.rest.CreateMailConfigurationRequest;
import com.example.domain.api.chat_service_api.integration.dto.rest.CreateTelegramConfigurationRequest;
import com.example.domain.api.chat_service_api.integration.dto.rest.CreateVkConfigurationRequest;
import com.example.domain.api.chat_service_api.integration.dto.rest.CreateWhatsappConfigurationRequest;
import com.example.domain.api.chat_service_api.integration.mapper.IntegrationMapper;
import com.example.domain.api.chat_service_api.integration.service.IIntegrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.AccessDeniedException;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/ui/integration")
@RequiredArgsConstructor
public class IntegrationController {

    private final IIntegrationService integrationService;
    private final IntegrationMapper integrationMapper;

    @PostMapping("/telegram")
    public ResponseEntity<IntegrationTelegramDto> createOrUpdateTelegramIntegration(@RequestBody CreateTelegramConfigurationRequest request) {
        try {
            CompanyTelegramConfiguration config = integrationService.createCompanyTelegramConfiguration(request);
            return ResponseEntity.ok(integrationMapper.toTelegramDto(config));
        } catch (AccessDeniedException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access Denied", e);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }

    @GetMapping("/telegram")
    public ResponseEntity<List<IntegrationTelegramDto>> getAllTelegramIntegrations() {
        try {
            List<CompanyTelegramConfiguration> configs = integrationService.getAllTelegramConfigurations();
            List<IntegrationTelegramDto> dtoList = configs.stream()
                    .map(integrationMapper::toTelegramDto)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(dtoList);
        } catch (AccessDeniedException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access Denied", e);
        }
    }

    @GetMapping("/telegram/{id}")
    public ResponseEntity<IntegrationTelegramDto> getTelegramIntegrationById(@PathVariable Integer id) {
        try {
            CompanyTelegramConfiguration config = integrationService.getTelegramConfigurationById(id);
            return ResponseEntity.ok(integrationMapper.toTelegramDto(config));
        } catch (AccessDeniedException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access Denied", e);
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        }
    }

    @DeleteMapping("/telegram/{id}")
    public ResponseEntity<Void> deleteTelegramIntegration(@PathVariable Integer id) {
        try {
            integrationService.deleteTelegramConfiguration(id);
            return ResponseEntity.noContent().build();
        } catch (AccessDeniedException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access Denied", e);
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        }
    }

    @PostMapping("/mail")
    public ResponseEntity<IntegrationMailDto> createOrUpdateMailIntegration(@RequestBody CreateMailConfigurationRequest request) {
        try {
            CompanyMailConfiguration config = integrationService.createCompanyMailConfiguration(request);
            return ResponseEntity.ok(integrationMapper.toMailDto(config));
        } catch (AccessDeniedException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access Denied", e);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }

    @GetMapping("/mail")
    public ResponseEntity<List<IntegrationMailDto>> getAllMailIntegrations() {
        try {
            List<CompanyMailConfiguration> configs = integrationService.getAllMailConfigurations();
            List<IntegrationMailDto> dtoList = integrationMapper.toMailDtoList(configs);
            return ResponseEntity.ok(dtoList);
        } catch (AccessDeniedException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access Denied", e);
        }
    }

    @GetMapping("/mail/{id}")
    public ResponseEntity<IntegrationMailDto> getMailIntegrationById(@PathVariable Integer id) {
        try {
            CompanyMailConfiguration config = integrationService.getMailConfigurationById(id);
            return ResponseEntity.ok(integrationMapper.toMailDto(config));
        } catch (AccessDeniedException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access Denied", e);
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        }
    }

    @DeleteMapping("/mail/{id}")
    public ResponseEntity<Void> deleteMailIntegration(@PathVariable Integer id) {
        try {
            integrationService.deleteMailConfiguration(id);
            return ResponseEntity.noContent().build();
        } catch (AccessDeniedException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access Denied", e);
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        }
    }

    @PostMapping("/whatsapp")
    public ResponseEntity<IntegrationWhatsappDto> createOrUpdateWhatsappIntegration(@RequestBody CreateWhatsappConfigurationRequest request) {
        try {
            CompanyWhatsappConfiguration config = integrationService.createCompanyWhatsappConfiguration(request);
            return ResponseEntity.ok(integrationMapper.toWhatsappDto(config));
        } catch (AccessDeniedException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access Denied", e);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }

    @GetMapping("/whatsapp")
    public ResponseEntity<List<IntegrationWhatsappDto>> getAllWhatsappIntegrations() {
        try {
            List<CompanyWhatsappConfiguration> configs = integrationService.getAllWhatsappConfigurations();
            List<IntegrationWhatsappDto> dtoList = configs.stream()
                    .map(integrationMapper::toWhatsappDto)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(dtoList);
        } catch (AccessDeniedException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access Denied", e);
        }
    }

    @GetMapping("/whatsapp/{id}")
    public ResponseEntity<IntegrationWhatsappDto> getWhatsappIntegrationById(@PathVariable Integer id) {
        try {
            CompanyWhatsappConfiguration config = integrationService.getWhatsappConfigurationById(id);
            return ResponseEntity.ok(integrationMapper.toWhatsappDto(config));
        } catch (AccessDeniedException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access Denied", e);
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        }
    }

    @DeleteMapping("/whatsapp/{id}")
    public ResponseEntity<Void> deleteWhatsappIntegration(@PathVariable Integer id) {
        try {
            integrationService.deleteWhatsappConfiguration(id);
            return ResponseEntity.noContent().build();
        } catch (AccessDeniedException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access Denied", e);
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        }
    }

    @PostMapping("/vk")
    public ResponseEntity<IntegrationVkDto> createOrUpdateVkIntegration(@RequestBody CreateVkConfigurationRequest request) {
        try {
            CompanyVkConfiguration config = integrationService.createCompanyVkConfiguration(request);
            return ResponseEntity.ok(integrationMapper.toVkDto(config));
        } catch (AccessDeniedException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access Denied", e);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }

    @GetMapping("/vk")
    public ResponseEntity<List<IntegrationVkDto>> getAllVkIntegrations() {
        try {
            List<CompanyVkConfiguration> configs = integrationService.getAllVkConfigurations();
            List<IntegrationVkDto> dtoList = configs.stream()
                    .map(integrationMapper::toVkDto)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(dtoList);
        } catch (AccessDeniedException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access Denied", e);
        }
    }

    @GetMapping("/vk/{id}")
    public ResponseEntity<IntegrationVkDto> getVkIntegrationById(@PathVariable Integer id) {
        try {
            CompanyVkConfiguration config = integrationService.getVkConfigurationById(id);
            return ResponseEntity.ok(integrationMapper.toVkDto(config));
        } catch (AccessDeniedException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access Denied", e);
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        }
    }

    @DeleteMapping("/vk/{id}")
    public ResponseEntity<Void> deleteVkIntegration(@PathVariable Integer id) {
        try {
            integrationService.deleteVkConfiguration(id);
            return ResponseEntity.noContent().build();
        } catch (AccessDeniedException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access Denied", e);
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        }
    }

}
