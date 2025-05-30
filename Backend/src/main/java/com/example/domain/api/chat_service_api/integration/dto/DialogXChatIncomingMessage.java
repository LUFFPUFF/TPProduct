package com.example.domain.api.chat_service_api.integration.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class DialogXChatIncomingMessage {

    @NotBlank
    private String widgetId;

    @NotBlank
    private String sessionId;

    @NotBlank
    @Size(max = 4000)
    private String text;

    private Long clientTimestamp;
}
