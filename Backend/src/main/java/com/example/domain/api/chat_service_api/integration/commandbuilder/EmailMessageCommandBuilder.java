package com.example.domain.api.chat_service_api.integration.commandbuilder;

import com.example.database.model.chats_messages_module.chat.Chat;
import com.example.database.model.chats_messages_module.chat.ChatChannel;
import com.example.database.model.company_subscription_module.company.CompanyMailConfiguration;
import com.example.database.repository.company_subscription_module.CompanyMailConfigurationRepository;
import com.example.domain.api.chat_service_api.exception_handler.ResourceNotFoundException;
import com.example.domain.api.chat_service_api.integration.listener.model.SendMessageCommand;
import com.example.domain.api.chat_service_api.integration.manager.widget.model.SenderInfoWidgetChat;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EmailMessageCommandBuilder implements MessageCommandBuilder {

    private final CompanyMailConfigurationRepository mailConfigRepository;

    @Override
    public ChatChannel getSupportedChannel() {
        return ChatChannel.Email;
    }

    @Override
    public SendMessageCommand buildCommand(Chat chat, String messageContent, SenderInfoWidgetChat senderInfo)
            throws ResourceNotFoundException {
        CompanyMailConfiguration mailConfig = mailConfigRepository
                .findByCompany(chat.getCompany())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Email configuration not found for company ID " + chat.getCompany().getId()));

        String clientEmailAddress = chat.getClient().getName();
        if (clientEmailAddress == null || clientEmailAddress.trim().isEmpty()){
            throw new IllegalArgumentException("Client email address (client.name) is missing for chat ID " + chat.getId());
        }

        String emailSubject = "Re: Чат #" + chat.getId() + " (" + chat.getCompany().getName() + ")";

        return SendMessageCommand.builder()
                .channel(ChatChannel.Email)
                .chatId(chat.getId())
                .companyId(chat.getCompany().getId())
                .content(messageContent)
                .toEmailAddress(clientEmailAddress)
                .fromEmailAddress(mailConfig.getEmailAddress())
                .subject(emailSubject)
                .senderType(senderInfo != null ? senderInfo.getSenderType() : null)
                .senderId(senderInfo != null ? senderInfo.getId() : null)
                .senderDisplayName(senderInfo != null ? senderInfo.getDisplayName() : null)
                .build();
    }
}
