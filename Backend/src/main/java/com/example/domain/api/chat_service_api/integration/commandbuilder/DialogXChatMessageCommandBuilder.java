package com.example.domain.api.chat_service_api.integration.commandbuilder;

import com.example.database.model.chats_messages_module.chat.Chat;
import com.example.database.model.chats_messages_module.chat.ChatChannel;
import com.example.database.model.company_subscription_module.company.CompanyDialogXChatConfiguration;
import com.example.database.repository.company_subscription_module.CompanyDialogXChatConfigurationRepository;
import com.example.domain.api.chat_service_api.exception_handler.ResourceNotFoundException;
import com.example.domain.api.chat_service_api.integration.listener.model.SendMessageCommand;
import com.example.domain.api.chat_service_api.integration.manager.widget.model.SenderInfoWidgetChat;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DialogXChatMessageCommandBuilder implements MessageCommandBuilder {

    private final CompanyDialogXChatConfigurationRepository dialogXChatConfigRepository;

    @Override
    public ChatChannel getSupportedChannel() {
        return ChatChannel.DialogX_Chat;
    }

    @Override
    public SendMessageCommand buildCommand(Chat chat, String messageContent, SenderInfoWidgetChat senderInfo)
            throws ResourceNotFoundException {
        CompanyDialogXChatConfiguration chatConfig = dialogXChatConfigRepository
                .findByCompanyId(chat.getCompany().getId())
                .filter(CompanyDialogXChatConfiguration::isEnabled)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Active DialogXChat configuration not found for company " + chat.getCompany().getId()));

        String clientWidgetSessionId = getClientWidgetSessionId(chatConfig, chat);

        if (senderInfo == null) {
            throw new IllegalArgumentException("SenderInfo cannot be null for DialogX_Chat messages.");
        }

        return SendMessageCommand.builder()
                .channel(ChatChannel.DialogX_Chat)
                .chatId(chat.getId())
                .companyId(chat.getCompany().getId())
                .content(messageContent)
                .dialogXChatSessionId(clientWidgetSessionId)
                .senderType(senderInfo.getSenderType())
                .senderId(senderInfo.getId())
                .senderDisplayName(senderInfo.getDisplayName())
                .build();
    }

    private static @NotNull String getClientWidgetSessionId(CompanyDialogXChatConfiguration configuration, Chat chat) {
        String clientWidgetSessionId = configuration.getWidgetId();
        if (clientWidgetSessionId == null || clientWidgetSessionId.trim().isEmpty()) {
            clientWidgetSessionId = chat.getClient().getName();
            if (clientWidgetSessionId == null || clientWidgetSessionId.trim().isEmpty()){
                throw new IllegalArgumentException(
                        "Client session ID for DialogXChat not found in chat.externalChatId or client.name for chat ID " + chat.getId());
            }
        }
        return clientWidgetSessionId;
    }
}
