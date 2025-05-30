package com.example.domain.api.chat_service_api.integration.sender;

import com.example.domain.api.chat_service_api.integration.exception.ChannelSenderException;
import com.example.domain.api.chat_service_api.integration.listener.model.SendMessageCommand;
import com.example.domain.api.chat_service_api.integration.manager.whats_app.WhatsappBotManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component("whatsappChannelSender")
@RequiredArgsConstructor
@Slf4j
public class WhatsappChannelSender implements ChannelSender {

    private final WhatsappBotManager whatsappBotManager;

    @Override
    public void send(SendMessageCommand command) throws ChannelSenderException {
        String recipientPhoneNumber = command.getWhatsappRecipientPhoneNumber();
        Integer companyId = command.getCompanyId();
        String content = command.getContent();

        if (recipientPhoneNumber == null || recipientPhoneNumber.trim().isEmpty()) {
            String errorMsg = String.format("WhatsApp recipient phone number not found in SendMessageCommand for company ID %d, chat ID %d",
                    companyId, command.getChatId());
            log.error(errorMsg);
            throw new ChannelSenderException(errorMsg);
        }

        if (content == null || content.trim().isEmpty()) {
            log.warn("Attempting to send empty or null content via WhatsApp for company ID {}, phone {}. Skipping.", companyId, recipientPhoneNumber);
            return;
        }

        try {
            whatsappBotManager.sendWhatsappMessage(companyId, recipientPhoneNumber, content);
            log.info("Message sent via WhatsappBotManager to phone {} (company ID {})", recipientPhoneNumber, companyId);
        } catch (Exception e) {
            String errorMsg = String.format("Failed to send WhatsApp message for company ID %d, chat ID %d, phone %s: %s",
                    companyId, command.getChatId(), recipientPhoneNumber, e.getMessage());
            log.error(errorMsg, e);
            throw new ChannelSenderException(errorMsg, e);
        }
    }
}
