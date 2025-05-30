package com.example.domain.api.chat_service_api.integration.manager.whats_app.model.response;

import lombok.Data;

import java.util.List;

@Data
public class WhatsappSendMessageResponse {

    private List<WhatsappSentMessage> messages;

    @Data
    public static class WhatsappSentMessage {
        private String id;
    }
}
