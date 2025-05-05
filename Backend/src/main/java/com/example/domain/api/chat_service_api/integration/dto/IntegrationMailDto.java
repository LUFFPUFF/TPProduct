package com.example.domain.api.chat_service_api.integration.dto;

import com.example.domain.dto.CompanyDto;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class IntegrationMailDto {

    private Integer id;
    private CompanyDto companyDto;
    private String emailAddress;
    private String appPassword;
    private String imapServer;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
