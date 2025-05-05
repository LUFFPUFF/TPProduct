package com.example.domain.api.chat_service_api.integration.telegram;

import lombok.Data;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;

@Data
public class TelegramApiResponse {
    private boolean ok;
    private List<Update> result;
}
