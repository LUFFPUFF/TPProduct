package com.example.domain.api.chat_service_api.integration.manager.vk.reponse;

import com.example.domain.api.chat_service_api.integration.manager.vk.model.VkApiError;
import lombok.Data;

@Data
public class VkApiResponse<T> {
    private T response;
    private VkApiError error;

    public boolean isOk() {
        return response != null && error == null;
    }
}
