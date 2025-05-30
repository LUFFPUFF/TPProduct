package com.example.domain.api.chat_service_api.integration.manager.vk.reponse;

import com.example.domain.api.chat_service_api.integration.manager.vk.model.VkLongPollUpdate;
import lombok.Data;

import java.util.List;

@Data
public class VkLongPollUpdatesResponse {

    private Integer ts;
    private List<VkLongPollUpdate> updates;
}
