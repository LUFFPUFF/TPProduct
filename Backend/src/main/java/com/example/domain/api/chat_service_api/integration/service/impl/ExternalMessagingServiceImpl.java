package com.example.domain.api.chat_service_api.integration.service.impl;

import com.example.database.model.chats_messages_module.chat.Chat;
import com.example.database.model.chats_messages_module.chat.ChatChannel;
import com.example.database.model.company_subscription_module.company.Company;
import com.example.database.model.company_subscription_module.company.CompanyMailConfiguration;
import com.example.database.model.company_subscription_module.company.CompanyTelegramConfiguration;
import com.example.database.model.crm_module.client.Client;
import com.example.database.repository.chats_messages_module.ChatRepository;
import com.example.database.repository.company_subscription_module.CompanyMailConfigurationRepository;
import com.example.database.repository.company_subscription_module.CompanyTelegramConfigurationRepository;
import com.example.domain.api.chat_service_api.exception_handler.ChatNotFoundException;
import com.example.domain.api.chat_service_api.exception_handler.ResourceNotFoundException;
import com.example.domain.api.chat_service_api.exception_handler.exception.ExternalMessagingException;
import com.example.domain.api.chat_service_api.integration.listener.SendMessageCommand;
import com.example.domain.api.chat_service_api.integration.service.IExternalMessagingService;
import com.example.domain.api.chat_service_api.service.IChatMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.BlockingQueue;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExternalMessagingServiceImpl implements IExternalMessagingService {

    private final ChatRepository chatRepository;
    private final BlockingQueue<Object> outgoingMessageQueue;
    private final CompanyMailConfigurationRepository mailConfigRepository;
    private final CompanyTelegramConfigurationRepository companyTelegramConfigurationRepository;

    @Override
    @Transactional(readOnly = true)
    public void sendMessageToExternal(Integer chatId, String messageContent) throws ExternalMessagingException {
        if (messageContent == null || messageContent.trim().isEmpty()) {
            log.warn("Attempted to send empty external message for chat ID {}", chatId);
            return;
        }

        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> {
                    log.error("Chat ID {} not found for sending external message.", chatId);
                    return new ChatNotFoundException("Chat with ID " + chatId + " not found for sending external message.");
                });

        Client client = chat.getClient();
        if (client == null) {
            log.error("Client not associated with chat ID {} for sending external message.", chatId);
            throw new ResourceNotFoundException("Client not found for chat ID " + chatId);
        }

        ChatChannel channel = chat.getChatChannel();
        if (channel == null) {
            log.error("Chat channel is null for chat ID {}.", chatId);
            throw new ExternalMessagingException("Chat channel is not defined for chat ID " + chatId);
        }

        Company company = chat.getCompany();
        if (company == null || company.getId() == null) {
            log.error("Company not associated or has null ID with chat ID {} for external messaging config.", chatId);
            throw new ResourceNotFoundException("Company not found or invalid for chat ID " + chatId);
        }

        Object sendMessageCommand;
        try {
            switch (channel) {
                case Telegram -> {
                    CompanyTelegramConfiguration telegramConfiguration = companyTelegramConfigurationRepository
                            .findByCompanyId(company.getId())
                            .orElseThrow(() -> new ResourceNotFoundException("Telegram config not found for company " + company.getId()));

                    sendMessageCommand = SendMessageCommand.builder()
                            .channel(ChatChannel.Telegram)
                            .chatId(chatId)
                            .companyId(company.getId())
                            .content(messageContent)
                            .telegramChatId(telegramConfiguration.getChatTelegramId())
                            .toEmailAddress(null)
                            .fromEmailAddress(null)
                            .subject(null)
                            .build();
                }

                case Email -> {
                    String clientEmailAddress = client.getName();

                    CompanyMailConfiguration mailConfig = mailConfigRepository.findByCompany(company)
                            .orElseThrow(() -> {
                                log.error("Email configuration not found for company ID {} (chat ID {}).", company.getId(), chatId);
                                return new ResourceNotFoundException("Email configuration not found for company ID " + company.getId());
                            });

                    String fromEmailAddress  = mailConfig.getEmailAddress();
                    String emailSubject = "Company DialogX - чат с клиентом";

                    sendMessageCommand = SendMessageCommand.builder()
                            .channel(ChatChannel.Email)
                            .chatId(chatId)
                            .companyId(company.getId())
                            .content(messageContent)
                            .telegramChatId(null)
                            .toEmailAddress(clientEmailAddress)
                            .fromEmailAddress(fromEmailAddress)
                            .subject(emailSubject)
                            .build();
                }

                default -> {
                    log.error("Unsupported chat channel for external messaging: {}", channel);
                    throw new ExternalMessagingException("Unsupported chat channel for external messaging: " + channel);
                }
            }
        } catch (Exception e) {
            log.error("Failed to create external message command for chat ID {}: {}", chatId, e.getMessage(), e);
            if (e instanceof ExternalMessagingException) {
                throw (ExternalMessagingException) e;
            } else if (e instanceof ResourceNotFoundException) {
                throw (ResourceNotFoundException) e;
            } else {
                throw new ExternalMessagingException("Failed to create external message command for chat ID " + chatId, e);
            }
        }

        try {
            outgoingMessageQueue.put(sendMessageCommand);
            log.info("Put SendMessageCommand for chat ID {} (channel {}) into outgoing queue.", chatId, channel);
        } catch (InterruptedException e) {
            log.error("Failed to put external message command into outgoing queue for chat ID {}: {}", chatId, e.getMessage(), e);
            Thread.currentThread().interrupt();
            throw new ExternalMessagingException("Failed to put external message command into outgoing queue for chat ID " + chatId, e);
        }
    }
}
