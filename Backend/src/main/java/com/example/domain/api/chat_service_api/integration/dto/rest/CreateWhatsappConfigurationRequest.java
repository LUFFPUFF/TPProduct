package com.example.domain.api.chat_service_api.integration.dto.rest;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class CreateWhatsappConfigurationRequest {

    @JsonProperty("phoneNumberId")
    private Long phoneNumberId;

    @JsonProperty("accessToken")
    private String accessToken;

    @JsonProperty("verifyToken")
    private String verifyToken;
}
