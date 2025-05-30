package com.example.domain.api.chat_service_api.integration.commandbuilder;

import com.example.database.model.chats_messages_module.chat.Chat;
import com.example.database.model.chats_messages_module.chat.ChatChannel;
import com.example.database.model.company_subscription_module.company.CompanyTelegramConfiguration;
import com.example.database.repository.company_subscription_module.CompanyTelegramConfigurationRepository;
import com.example.domain.api.chat_service_api.exception_handler.ResourceNotFoundException;
import com.example.domain.api.chat_service_api.integration.listener.model.SendMessageCommand;
import com.example.domain.api.chat_service_api.integration.manager.widget.model.SenderInfoWidgetChat;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TelegramMessageCommandBuilder implements MessageCommandBuilder {

    private final CompanyTelegramConfigurationRepository telegramConfigurationRepository;

    @Override
    public ChatChannel getSupportedChannel() {
        return ChatChannel.Telegram;
    }

    @Override
    public SendMessageCommand buildCommand(Chat chat, String messageContent, SenderInfoWidgetChat senderInfo) throws ResourceNotFoundException, IllegalArgumentException {
        CompanyTelegramConfiguration telegramConfig = telegramConfigurationRepository
                .findByCompanyId(chat.getCompany().getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Telegram config not found for company " + chat.getCompany().getId()));

        if (telegramConfig.getChatTelegramId() == null) {
            throw new ResourceNotFoundException(
                    "Telegram chat ID not configured for company " + chat.getCompany().getId());
        }

        return SendMessageCommand.builder()
                .channel(ChatChannel.Telegram)
                .chatId(chat.getId())
                .companyId(chat.getCompany().getId())
                .content(messageContent)
                .telegramChatId(telegramConfig.getChatTelegramId())
                .senderType(senderInfo != null ? senderInfo.getSenderType() : null)
                .senderId(senderInfo != null ? senderInfo.getId() : null)
                .senderDisplayName(senderInfo != null ? senderInfo.getDisplayName() : null)
                .build();
    }
}
