package com.example.domain.api.chat_service_api.integration.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class IncomingTelegramMessage {

    private Long botId;
    private String botUsername;
    private String username;
    private String firstUsername;
    private String text;
    private Integer date;

    private String externalMessageId;
    private String replyToExternalMessageId;
    private Long telegramChatId;

}
