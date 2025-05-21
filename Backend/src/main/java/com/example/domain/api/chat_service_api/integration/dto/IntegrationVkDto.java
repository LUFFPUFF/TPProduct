package com.example.domain.api.chat_service_api.integration.dto;

import com.example.domain.dto.CompanyDto;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class IntegrationVkDto {

    private Integer id;
    private CompanyDto companyDto;
    private Long communityId;
    private String communityName;
    private LocalDateTime createdAt;
    private boolean active;
}
