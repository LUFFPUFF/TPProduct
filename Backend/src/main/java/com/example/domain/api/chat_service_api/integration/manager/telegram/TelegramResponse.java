package com.example.domain.api.chat_service_api.integration.manager.telegram;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.springframework.stereotype.Component;

@Data
@AllArgsConstructor
@Builder
public class TelegramResponse{

    private Long botId;
    private String botUsername;
    private String username;
    private String firstUsername;
    private String text;
    private Integer date;
    private Long chatId;
}
