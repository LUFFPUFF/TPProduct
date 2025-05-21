package com.example.domain.api.chat_service_api.integration.dto.rest;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class CreateVkConfigurationRequest {

    @JsonProperty("communityId")
    private Long communityId;

    @JsonProperty("communityName")
    private String communityName;

    @JsonProperty("accessToken")
    private String accessToken;
}
