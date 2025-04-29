package com.example.domain.api.chat_service_api.integration.telegram;

import com.example.database.repository.company_subscription_module.CompanyTelegramConfigurationRepository;
import org.springframework.stereotype.Component;

@Component
public class TelegramConfiguration {

    private final CompanyTelegramConfigurationRepository configurationRepository;

    public TelegramConfiguration(CompanyTelegramConfigurationRepository configurationRepository) {
        this.configurationRepository = configurationRepository;
    }

    public String getBotToken(String botUsername) {
        return configurationRepository.findByBotUsername(botUsername)
                .get()
                .getBotToken();
    }

}
