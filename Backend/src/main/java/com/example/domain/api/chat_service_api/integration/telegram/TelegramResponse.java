package com.example.domain.api.chat_service_api.integration.telegram;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TelegramResponse{

    private Long botId;
    private String botUsername;
    private String username;
    private String firstUsername;
    private String text;
    private Integer date;
}
