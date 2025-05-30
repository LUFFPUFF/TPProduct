package com.example.domain.api.chat_service_api.integration.sender;

import com.example.domain.api.chat_service_api.integration.exception.ChannelSenderException;
import com.example.domain.api.chat_service_api.integration.listener.model.SendMessageCommand;
import com.example.domain.api.chat_service_api.integration.manager.telegram.TelegramBotManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component("telegramChannelSender")
@RequiredArgsConstructor
public class TelegramChannelSender implements ChannelSender{

    private final TelegramBotManager telegramBotManager;

    private static final String TELEGRAM_EXCEPTION = "Telegram Chat ID not found in SendMessageCommand for company ID %d, chat ID %d";

    @Override
    public void send(SendMessageCommand command) throws ChannelSenderException {
        Long telegramChatId = command.getTelegramChatId();
        Integer companyId = command.getCompanyId();

        if (telegramChatId == null) {
            String errorMsg = String.format(TELEGRAM_EXCEPTION, companyId, command.getChatId());
            log.error(errorMsg);
            throw new ChannelSenderException(errorMsg);
        }

        try {
            telegramBotManager.sendTelegramMessage(companyId, telegramChatId, command.getContent());
            log.info("Message sent via TelegramBotManager for Telegram chat ID {} (company ID {})", telegramChatId, companyId);
        } catch (Exception e) {
            String errorMsg = String.format("Failed to send Telegram message for company ID %d, chat ID %d, telegramChatId %d: %s",
                    companyId, command.getChatId(), telegramChatId, e.getMessage());
            log.error(errorMsg, e);
            throw new ChannelSenderException(errorMsg, e);
        }
    }
}
