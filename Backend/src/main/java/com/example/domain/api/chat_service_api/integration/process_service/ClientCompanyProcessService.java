package com.example.domain.api.chat_service_api.integration.process_service;

import com.example.database.model.chats_messages_module.chat.Chat;
import com.example.database.model.chats_messages_module.chat.ChatChannel;
import com.example.database.model.chats_messages_module.chat.ChatMessageSenderType;
import com.example.database.model.chats_messages_module.chat.ChatStatus;
import com.example.database.model.company_subscription_module.company.*;
import com.example.database.model.company_subscription_module.user_roles.user.User;
import com.example.database.model.crm_module.client.Client;
import com.example.database.repository.chats_messages_module.ChatRepository;
import com.example.database.repository.company_subscription_module.CompanyMailConfigurationRepository;
import com.example.database.repository.company_subscription_module.CompanyTelegramConfigurationRepository;
import com.example.database.repository.company_subscription_module.CompanyVkConfigurationRepository;
import com.example.database.repository.company_subscription_module.CompanyWhatsappConfigurationRepository;
import com.example.domain.api.ans_api_module.exception.AutoResponderException;
import com.example.domain.api.ans_api_module.service.IAutoResponderService;
import com.example.domain.api.chat_service_api.exception_handler.ResourceNotFoundException;
import com.example.domain.api.chat_service_api.exception_handler.exception.service.ChatServiceException;
import com.example.domain.api.chat_service_api.integration.mail.response.EmailResponse;
import com.example.domain.api.chat_service_api.integration.telegram.TelegramResponse;
import com.example.domain.api.chat_service_api.integration.vk.reponse.VkResponse;
import com.example.domain.api.chat_service_api.integration.whats_app.model.response.WhatsappResponse;
import com.example.domain.api.chat_service_api.model.rest.chat.CreateChatRequestDTO;
import com.example.domain.api.chat_service_api.model.rest.mesage.SendMessageRequestDTO;
import com.example.domain.api.chat_service_api.service.*;
import com.example.domain.api.chat_service_api.model.dto.MessageDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClientCompanyProcessService {

    private final CompanyTelegramConfigurationRepository telegramConfigurationRepository;
    private final CompanyMailConfigurationRepository gmailConfigurationRepository;
    private final CompanyVkConfigurationRepository vkConfigurationRepository;
    private final CompanyWhatsappConfigurationRepository whatsappConfigurationRepository;
    private final IClientService clientService;
    private final IChatService chatService;
    private final IChatMessageService chatMessageService;
    private final IAutoResponderService autoResponderService;
    private final IAssignmentService assignmentService;
    private final ChatRepository chatRepository;

    private static final int MAX_CONTENT_LENGTH = 255;
    private static final String TRUNCATE_INDICATOR = "...";

    @Transactional
    public void processTelegram(TelegramResponse telegramResponse) {
        CompanyTelegramConfiguration configuration =
                telegramConfigurationRepository.findByBotUsername(telegramResponse.getBotUsername())
                        .orElseThrow(() -> new ResourceNotFoundException("Telegram configuration not found"));

        Company company = configuration.getCompany();
        String telegramUsername = telegramResponse.getUsername();

        Client client = clientService.findByName(telegramUsername)
                .orElseGet(() -> clientService.createClient(telegramUsername, company.getId(), null));

        Optional<Chat> existingOpenChat = chatService.findOpenChatByClientAndChannel(client.getId(), ChatChannel.Telegram);

        if (existingOpenChat.isPresent()) {
            Chat chat = existingOpenChat.get();

            SendMessageRequestDTO messageRequest = new SendMessageRequestDTO();
            messageRequest.setChatId(existingOpenChat.get().getId());
            messageRequest.setContent(telegramResponse.getText());
            messageRequest.setSenderId(client.getId());
            messageRequest.setSenderType(ChatMessageSenderType.CLIENT);

            MessageDto messageDto = chatMessageService.processAndSaveMessage(
                    messageRequest,
                    messageRequest.getSenderId(),
                    messageRequest.getSenderType()
            );

            if (containsOperatorRequest(telegramResponse.getText())) {
                log.info("Operator request detected in chat {}", chat.getId());
                assignOperatorToChat(chat);
            } else {
                processAutoResponder(chat, messageDto);
            }

            log.info("Processed incoming message for chat ID {}.", chat.getId() + " - " + messageDto);
        } else {
            CreateChatRequestDTO createChatRequest = new CreateChatRequestDTO();
            createChatRequest.setClientId(client.getId());
            createChatRequest.setCompanyId(company.getId());
            createChatRequest.setChatChannel(ChatChannel.Telegram);
            createChatRequest.setInitialMessageContent(telegramResponse.getText());

            chatService.createChat(createChatRequest);
        }

    }

    @Transactional
    public void processEmail(String companyEmailAddress, EmailResponse emailResponse) {
        log.info("Processing incoming email for account: {}", companyEmailAddress);
        CompanyMailConfiguration mailConfiguration =
                gmailConfigurationRepository.findByEmailAddress(companyEmailAddress)
                        .orElseThrow(() -> new ResourceNotFoundException("Email configuration not found for address: " + companyEmailAddress));

        Company company = mailConfiguration.getCompany();
        String clientEmail = emailResponse.getFrom();

        Client client = clientService.findByName(clientEmail)
                .orElseGet(() -> {
                    log.info("Creating new client for email address: {}", clientEmail);
                    return clientService.createClient(clientEmail, company.getId(), null);
                });

        Optional<Chat> existingOpenChat = chatService.findOpenChatByClientAndChannel(client.getId(), ChatChannel.Email);

        String fullEmailContent = formatEmailContent(emailResponse);

        if (existingOpenChat.isPresent()) {
            Chat chat = existingOpenChat.get();
            log.info("Processing incoming email for existing chat ID {}.", chat.getId());

            SendMessageRequestDTO messageRequest = new SendMessageRequestDTO();
            messageRequest.setChatId(chat.getId());
            messageRequest.setContent(truncateContent(fullEmailContent, MAX_CONTENT_LENGTH, TRUNCATE_INDICATOR));
            messageRequest.setSenderId(client.getId());
            messageRequest.setSenderType(ChatMessageSenderType.CLIENT);
            // TODO: Возможно, стоит сохранить полное содержимое письма в отдельном поле или в файле, если оно длинное

            MessageDto messageDto = chatMessageService.processAndSaveMessage(messageRequest,
                    messageRequest.getSenderId(),
                    messageRequest.getSenderType());

            if (containsOperatorRequest(emailResponse.getContent())) {
                log.info("Operator request detected in Email chat {}", chat.getId());
                assignOperatorToChat(chat);
            } else {
                processAutoResponder(chat, messageDto);
            }
            log.info("Processed incoming email message (chat ID {}).", chat.getId());

        } else {
            log.info("Creating new chat for incoming email from {}.", clientEmail);
            CreateChatRequestDTO createChatRequest = new CreateChatRequestDTO();
            createChatRequest.setClientId(client.getId());
            createChatRequest.setCompanyId(company.getId());
            createChatRequest.setChatChannel(ChatChannel.Email);
            createChatRequest.setInitialMessageContent(truncateContent(fullEmailContent, MAX_CONTENT_LENGTH, TRUNCATE_INDICATOR));

            chatService.createChat(createChatRequest);
            log.info("Created new chat for email client {}.", clientEmail);
        }
    }

    @Transactional
    public void processVk(VkResponse vkResponse) {
        CompanyVkConfiguration configuration =
                vkConfigurationRepository.findByCommunityId(vkResponse.getCommunityId())
                        .orElseThrow(() -> new ResourceNotFoundException("VK configuration not found for community ID: " + vkResponse.getCommunityId()));

        Company company = configuration.getCompany();
        Long vkUserId = vkResponse.getFromId();

        Client client = clientService.findByName(vkUserId.toString())
                .orElseGet(() -> {
                    log.info("Creating new client for VK user ID: {}", vkUserId);
                    return clientService.createClient(vkUserId.toString(), company.getId(), null);
                });

        Optional<Chat> existingOpenChat = chatService.findOpenChatByClientAndChannelAndExternalId(
                client.getId(), ChatChannel.VK, vkResponse.getPeerId().toString());

        String fullMessageContent = vkResponse.getText();

        if (existingOpenChat.isPresent()) {
            Chat chat = existingOpenChat.get();
            log.info("Processing incoming VK message for existing chat ID {}.", chat.getId());

            SendMessageRequestDTO messageRequest = new SendMessageRequestDTO();
            messageRequest.setChatId(chat.getId());
            messageRequest.setContent(truncateContent(fullMessageContent, MAX_CONTENT_LENGTH, TRUNCATE_INDICATOR));
            messageRequest.setSenderId(client.getId());
            messageRequest.setSenderType(ChatMessageSenderType.CLIENT);
            // TODO: Если текст очень длинный, сохранить его полностью где-то еще

            MessageDto messageDto = chatMessageService.processAndSaveMessage(
                    messageRequest,
                    messageRequest.getSenderId(),
                    messageRequest.getSenderType()
            );

            if (containsOperatorRequest(vkResponse.getText())) {
                log.info("Operator request detected in VK chat {}", chat.getId());
                assignOperatorToChat(chat);
            } else {
                processAutoResponder(chat, messageDto);
            }

        } else {
            log.info("Creating new chat for incoming VK message from user {}.", vkUserId);
            CreateChatRequestDTO createChatRequest = new CreateChatRequestDTO();
            createChatRequest.setClientId(client.getId());
            createChatRequest.setCompanyId(company.getId());
            createChatRequest.setChatChannel(ChatChannel.VK);
            createChatRequest.setInitialMessageContent(truncateContent(fullMessageContent, MAX_CONTENT_LENGTH, TRUNCATE_INDICATOR));
            createChatRequest.setExternalChatId(vkResponse.getPeerId().toString());

            chatService.createChat(createChatRequest);
            log.info("Created new chat for VK client {} (peer ID {}).", vkUserId, vkResponse.getPeerId());
        }
    }

    @Transactional
    public void processWhatsapp(WhatsappResponse whatsappResponse) {
        log.info("Processing incoming WhatsApp message from phone {} to phone ID {}",
                whatsappResponse.getFromPhoneNumber(), whatsappResponse.getRecipientPhoneNumberId());

        CompanyWhatsappConfiguration configuration =
                whatsappConfigurationRepository.findByPhoneNumberId(whatsappResponse.getRecipientPhoneNumberId())
                        .orElseThrow(() -> new ResourceNotFoundException("WhatsApp configuration not found for phone number ID: " + whatsappResponse.getRecipientPhoneNumberId()));

        Company company = configuration.getCompany();
        String fromPhoneNumber = whatsappResponse.getFromPhoneNumber();

        Client client = clientService.findByName(fromPhoneNumber)
                .orElseGet(() -> {
                    log.info("Creating new client for WhatsApp phone number: {}", fromPhoneNumber);
                    return clientService.createClient(fromPhoneNumber, company.getId(), null);
                });

        Optional<Chat> existingOpenChat = chatService.findOpenChatByClientAndChannel(client.getId(), ChatChannel.WhatsApp);

        String fullMessageContent = whatsappResponse.getText();

        if (existingOpenChat.isPresent()) {
            Chat chat = existingOpenChat.get();

            SendMessageRequestDTO messageRequest = new SendMessageRequestDTO();
            messageRequest.setChatId(chat.getId());
            messageRequest.setContent(truncateContent(fullMessageContent, MAX_CONTENT_LENGTH, TRUNCATE_INDICATOR));
            messageRequest.setSenderId(client.getId());
            messageRequest.setSenderType(ChatMessageSenderType.CLIENT);

            MessageDto messageDto = chatMessageService.processAndSaveMessage(
                    messageRequest,
                    messageRequest.getSenderId(),
                    messageRequest.getSenderType()
            );

            if (containsOperatorRequest(whatsappResponse.getText())) {
                log.info("Operator request detected in WhatsApp chat {}", chat.getId());
                assignOperatorToChat(chat);
            } else {
                processAutoResponder(chat, messageDto);
            }

        } else {
            log.info("Creating new chat for incoming WhatsApp message from {}.", fromPhoneNumber);
            CreateChatRequestDTO createChatRequest = new CreateChatRequestDTO();
            createChatRequest.setClientId(client.getId());
            createChatRequest.setCompanyId(company.getId());
            createChatRequest.setChatChannel(ChatChannel.WhatsApp);
            createChatRequest.setInitialMessageContent(truncateContent(fullMessageContent, MAX_CONTENT_LENGTH, TRUNCATE_INDICATOR));

            chatService.createChat(createChatRequest);
            log.info("Created new chat for WhatsApp client {} (phone {}).", client.getId(), fromPhoneNumber);
        }
    }

    private String truncateContent(String content, int maxLength, String truncateIndicator) {
        if (content == null) {
            return null;
        }
        if (content.length() <= maxLength) {
            return content;
        }
        if (maxLength <= truncateIndicator.length()) {
            return "";
        }
        String truncated = content.substring(0, maxLength - truncateIndicator.length());
        return truncated + truncateIndicator;
    }

    private String formatEmailContent(EmailResponse emailResponse) {
        return "Subject: " + emailResponse.getSubject() + "\n\n" + emailResponse.getContent();
    }

    private void processAutoResponder(Chat chat, MessageDto messageDto) {
        if (chat.getStatus() == ChatStatus.PENDING_AUTO_RESPONDER) {
            try {
                autoResponderService.processIncomingMessage(messageDto, chat);
            } catch (Exception e) {
                log.error("AutoResponder processing failed for chat {}: {}", chat.getId(), e.getMessage());
                throw new AutoResponderException("autoResponder processing failed", e);
            }
        }
    }

    //TODO такие действия лучше скормить нейронке
    private boolean containsOperatorRequest(String message) {
        if (message == null || message.isBlank()) {
            return false;
        }

        String lowerCaseText = message.toLowerCase();
        return lowerCaseText.contains("оператор") ||
                lowerCaseText.contains("operator") ||
                lowerCaseText.contains("помощь") ||
                lowerCaseText.contains("help") ||
                lowerCaseText.contains("человек");
    }

    private void assignOperatorToChat(Chat chat) {
        try {
            autoResponderService.stopForChat(chat.getId());

            Optional<User> operator = assignmentService.findLeastBusyOperator(chat.getCompany().getId());

            if (operator.isPresent()) {
                chat.setUser(operator.get());
                chat.setStatus(ChatStatus.ASSIGNED);
                chatRepository.save(chat);
                log.info("Operator {} assigned to chat {}", operator.get().getId(), chat.getId());
            } else {
                chat.setStatus(ChatStatus.PENDING_OPERATOR);
                chatRepository.save(chat);
                log.info("No available operators, chat {} set to PENDING_OPERATOR", chat.getId());
            }
        } catch (Exception e) {
            log.error("Failed to assign operator to chat {}: {}", chat.getId(), e.getMessage(), e);
            throw new ChatServiceException("Failed to assign operator to chat");
        }
    }
}
