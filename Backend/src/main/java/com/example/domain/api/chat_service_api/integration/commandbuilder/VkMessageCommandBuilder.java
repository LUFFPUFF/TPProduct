package com.example.domain.api.chat_service_api.integration.commandbuilder;

import com.example.database.model.chats_messages_module.chat.Chat;
import com.example.database.model.chats_messages_module.chat.ChatChannel;
import com.example.domain.api.chat_service_api.exception_handler.ResourceNotFoundException;
import com.example.domain.api.chat_service_api.integration.listener.model.SendMessageCommand;
import com.example.domain.api.chat_service_api.integration.manager.widget.model.SenderInfoWidgetChat;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class VkMessageCommandBuilder implements MessageCommandBuilder {

    @Override
    public ChatChannel getSupportedChannel() {
        return ChatChannel.VK;
    }

    @Override
    public SendMessageCommand buildCommand(Chat chat, String messageContent, SenderInfoWidgetChat senderInfo)
            throws ResourceNotFoundException, IllegalArgumentException {

        String vkPeerIdStr = chat.getExternalChatId();
        if (vkPeerIdStr == null || vkPeerIdStr.trim().isEmpty()) {
            log.error("External chat ID (VK peer ID) not found in chat {} for VK channel.", chat.getId());
            throw new IllegalArgumentException("VK peer ID (chat.externalChatId) is missing for VK chat.");
        }

        long vkPeerId;
        try {
            vkPeerId = Long.parseLong(vkPeerIdStr);
        } catch (NumberFormatException e) {
            log.error("Invalid VK peer ID format in chat {}: {}", chat.getId(), vkPeerIdStr);
            throw new IllegalArgumentException("Invalid VK peer ID format: " + vkPeerIdStr);
        }

        return SendMessageCommand.builder()
                .channel(ChatChannel.VK)
                .chatId(chat.getId())
                .companyId(chat.getCompany().getId())
                .content(messageContent)
                .vkPeerId(vkPeerId)
                .senderType(senderInfo != null ? senderInfo.getSenderType() : null)
                .senderId(senderInfo != null ? senderInfo.getId() : null)
                .senderDisplayName(senderInfo != null ? senderInfo.getDisplayName() : null)
                .build();
    }
}
