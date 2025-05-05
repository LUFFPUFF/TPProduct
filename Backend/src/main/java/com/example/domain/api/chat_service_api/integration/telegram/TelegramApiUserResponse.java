package com.example.domain.api.chat_service_api.integration.telegram;

import lombok.Data;
import org.telegram.telegrambots.meta.api.objects.User;

@Data
public class TelegramApiUserResponse {
    private boolean ok;
    private User result;
}
