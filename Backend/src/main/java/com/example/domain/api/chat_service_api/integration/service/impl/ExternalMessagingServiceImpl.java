package com.example.domain.api.chat_service_api.integration.service.impl;

import com.example.database.model.chats_messages_module.chat.Chat;
import com.example.database.model.chats_messages_module.chat.ChatChannel;
import com.example.database.repository.chats_messages_module.ChatRepository;
import com.example.domain.api.chat_service_api.exception_handler.ChatNotFoundException;
import com.example.domain.api.chat_service_api.exception_handler.ResourceNotFoundException;
import com.example.domain.api.chat_service_api.exception_handler.exception.ExternalMessagingException;
import com.example.domain.api.chat_service_api.integration.commandbuilder.MessageCommandBuilder;
import com.example.domain.api.chat_service_api.integration.listener.model.SendMessageCommand;
import com.example.domain.api.chat_service_api.integration.manager.widget.model.SenderInfoWidgetChat;
import com.example.domain.api.chat_service_api.integration.service.IExternalMessagingService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExternalMessagingServiceImpl implements IExternalMessagingService {

    private final ChatRepository chatRepository;
    private final BlockingQueue<Object> outgoingMessageQueue;
    private final List<MessageCommandBuilder> commandBuilders;
    private Map<ChatChannel, MessageCommandBuilder> builderStrategies;

    @PostConstruct
    public void initStrategies() {
        builderStrategies = new EnumMap<>(ChatChannel.class);
        for (MessageCommandBuilder builder : commandBuilders) {
            builderStrategies.put(builder.getSupportedChannel(), builder);
        }
        log.info("ExternalMessagingService initialized with command builder strategies for channels: {}", builderStrategies.keySet());
    }

    @Override
    @Transactional(readOnly = true)
    public void sendMessageToExternal(Integer chatId, String messageContent, SenderInfoWidgetChat senderInfo)
            throws ExternalMessagingException {
        if (messageContent == null || messageContent.trim().isEmpty()) {
            log.warn("Attempted to send empty external message for chat ID {}", chatId);
            return;
        }

        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new ChatNotFoundException("Chat with ID " + chatId + " not found."));

        validateChatPrerequisites(chat);

        ChatChannel channel = chat.getChatChannel();
        MessageCommandBuilder builder = Optional.ofNullable(builderStrategies.get(channel))
                .orElseThrow(() -> {
                    log.error("Unsupported chat channel for external messaging or no builder found: {}", channel);
                    return new ExternalMessagingException("Unsupported chat channel for external messaging: " + channel);
                });

        SendMessageCommand sendMessageCommand;
        try {
            sendMessageCommand = builder.buildCommand(chat, messageContent, senderInfo);
        } catch (ResourceNotFoundException | IllegalArgumentException e) {
            log.error("Failed to build SendMessageCommand for chat ID {} (channel {}): {}",
                    chatId, channel, e.getMessage(), e);
            throw new ExternalMessagingException("Failed to prepare message for channel " + channel + ": " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error building SendMessageCommand for chat ID {} (channel {}): {}",
                    chatId, channel, e.getMessage(), e);
            throw new ExternalMessagingException("Unexpected error preparing message for channel " + channel, e);
        }

        try {
            outgoingMessageQueue.put(sendMessageCommand);
            log.info("Put SendMessageCommand for chat ID {} (channel {}) into outgoing queue.", chatId, channel);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Failed to put SendMessageCommand into outgoing queue for chat ID {}: {}", chatId, e.getMessage(), e);
            throw new ExternalMessagingException("Failed to queue message for chat ID " + chatId, e);
        }
    }

    private void validateChatPrerequisites(Chat chat) throws ResourceNotFoundException {
        if (chat.getClient() == null) {
            log.error("Client not associated with chat ID {} for sending external message.", chat.getId());
            throw new ResourceNotFoundException("Client not found for chat ID " + chat.getId());
        }
        if (chat.getChatChannel() == null) {
            log.error("Chat channel is null for chat ID {}.", chat.getId());
            throw new ResourceNotFoundException("Chat channel is not defined for chat ID " + chat.getId());
        }
        if (chat.getCompany() == null || chat.getCompany().getId() == null) {
            log.error("Company not associated or has null ID with chat ID {} for external messaging config.", chat.getId());
            throw new ResourceNotFoundException("Company not found or invalid for chat ID " + chat.getId());
        }
    }
}
