package com.example.domain.api.chat_service_api.integration.vk.reponse;

import lombok.Data;

@Data
public class VkLongPollServerResponse {
    private String key;
    private String server;
    private String ts;
}
