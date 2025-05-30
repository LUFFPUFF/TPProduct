package com.example.domain.api.chat_service_api.integration.manager.whats_app.model.response;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class WhatsappResponse {

    private Integer companyId;
    private Long recipientPhoneNumberId;
    private String fromPhoneNumber;
    private String text;
    private String messageId;
    private Instant timestamp;
}
