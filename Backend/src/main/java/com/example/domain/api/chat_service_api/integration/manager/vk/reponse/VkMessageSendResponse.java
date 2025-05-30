package com.example.domain.api.chat_service_api.integration.manager.vk.reponse;

import lombok.Data;

import java.util.List;

@Data
public class VkMessageSendResponse {

    private List<Long> messageIds;
    private Long messageId;
}
