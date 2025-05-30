package com.example.domain.api.chat_service_api.integration.commandbuilder;

import com.example.database.model.chats_messages_module.chat.Chat;
import com.example.database.model.chats_messages_module.chat.ChatChannel;
import com.example.database.model.company_subscription_module.company.CompanyWhatsappConfiguration;
import com.example.database.repository.company_subscription_module.CompanyWhatsappConfigurationRepository;
import com.example.domain.api.chat_service_api.exception_handler.ResourceNotFoundException;
import com.example.domain.api.chat_service_api.integration.commandbuilder.MessageCommandBuilder;
import com.example.domain.api.chat_service_api.integration.listener.model.SendMessageCommand;
import com.example.domain.api.chat_service_api.integration.manager.widget.model.SenderInfoWidgetChat;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class WhatsappMessageCommandBuilder implements MessageCommandBuilder {

    private final CompanyWhatsappConfigurationRepository whatsappConfigRepository;

    @Override
    public ChatChannel getSupportedChannel() {
        return ChatChannel.WhatsApp;
    }

    @Override
    public SendMessageCommand buildCommand(Chat chat, String messageContent, SenderInfoWidgetChat senderInfo)
            throws ResourceNotFoundException, IllegalArgumentException {

        CompanyWhatsappConfiguration whatsappConfig = whatsappConfigRepository
                .findByCompanyIdAndAccessTokenIsNotNullAndAccessTokenIsNot(chat.getCompany().getId(), "")
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Active WhatsApp configuration not found for company " + chat.getCompany().getId()));

        String whatsappRecipientPhoneNumber = String.valueOf(whatsappConfig.getPhoneNumberId());
        if (whatsappRecipientPhoneNumber == null || whatsappRecipientPhoneNumber.trim().isEmpty()) {
            log.error("Client phone number (client.name) not found for chat ID {} (client ID {}). Cannot send WhatsApp message.",
                    chat.getId(), chat.getClient().getId());
            throw new IllegalArgumentException("Client phone number for WhatsApp messaging is missing.");
        }


        Integer finalChatId = chat.getId();

        return SendMessageCommand.builder()
                .channel(ChatChannel.WhatsApp)
                .chatId(finalChatId)
                .companyId(chat.getCompany().getId())
                .content(messageContent)
                .whatsappRecipientPhoneNumber(whatsappRecipientPhoneNumber)
                .senderType(senderInfo != null ? senderInfo.getSenderType() : null)
                .senderId(senderInfo != null ? senderInfo.getId() : null)
                .senderDisplayName(senderInfo != null ? senderInfo.getDisplayName() : null)
                .build();
    }
}
