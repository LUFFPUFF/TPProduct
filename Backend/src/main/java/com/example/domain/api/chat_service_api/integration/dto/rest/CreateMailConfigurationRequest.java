package com.example.domain.api.chat_service_api.integration.dto.rest;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CreateMailConfigurationRequest {

    @JsonProperty("emailAddress")
    private String emailAddress;

    @JsonProperty("appPassword")
    private String appPassword;
}
