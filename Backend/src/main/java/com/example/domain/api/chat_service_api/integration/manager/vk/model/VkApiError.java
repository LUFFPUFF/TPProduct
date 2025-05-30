package com.example.domain.api.chat_service_api.integration.manager.vk.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class VkApiError {
    @JsonProperty("error_code")
    private Integer errorCode;
    @JsonProperty("error_msg")
    private String errorMsg;
}
