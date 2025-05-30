package com.example.domain.api.chat_service_api.integration.sender;

import com.example.domain.api.chat_service_api.integration.exception.ChannelSenderException;
import com.example.domain.api.chat_service_api.integration.listener.model.SendMessageCommand;
import com.example.domain.api.chat_service_api.integration.manager.vk.VkBotManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component("vkChannelSender")
@RequiredArgsConstructor
@Slf4j
public class VkChannelSender implements ChannelSender {

    private final VkBotManager vkBotManager;

    @Override
    public void send(SendMessageCommand command) throws ChannelSenderException {
        Long vkPeerId = command.getVkPeerId();
        Integer companyId = command.getCompanyId();
        String content = command.getContent();

        if (vkPeerId == null) {
            String errorMsg = String.format("VK Peer ID not found in SendMessageCommand for company ID %d, chat ID %d",
                    companyId, command.getChatId());
            log.error(errorMsg);
            throw new ChannelSenderException(errorMsg);
        }

        if (content == null || content.trim().isEmpty()) {
            log.warn("Attempting to send empty or null content via VK for company ID {}, peer ID {}. Skipping.", companyId, vkPeerId);
            return;
        }

        try {
            vkBotManager.sendVkMessage(companyId, vkPeerId, content);
            log.info("Message sent via VkBotManager for VK peer ID {} (company ID {})", vkPeerId, companyId);
        } catch (Exception e) {
            String errorMsg = String.format("Failed to send VK message for company ID %d, chat ID %d, peer ID %d: %s",
                    companyId, command.getChatId(), vkPeerId, e.getMessage());
            log.error(errorMsg, e);
            throw new ChannelSenderException(errorMsg, e);
        }
    }
}
