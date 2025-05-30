package com.example.domain.api.chat_service_api.integration.manager.vk.model;

import lombok.Data;

@Data
public class VkLongPollUpdate {

    private String type;
    private VkLongPollObject object;
}
