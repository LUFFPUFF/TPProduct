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
import com.example.domain.api.chat_service_api.event.message.ChatMessageSentEvent;
import com.example.domain.api.chat_service_api.exception_handler.ResourceNotFoundException;
import com.example.domain.api.chat_service_api.integration.mail.response.EmailResponse;
import com.example.domain.api.chat_service_api.integration.telegram.TelegramResponse;
import com.example.domain.api.chat_service_api.integration.vk.reponse.VkResponse;
import com.example.domain.api.chat_service_api.integration.whats_app.model.response.WhatsappResponse;
import com.example.domain.api.chat_service_api.model.dto.ChatDetailsDTO;
import com.example.domain.api.chat_service_api.model.rest.chat.CreateChatRequestDTO;
import com.example.domain.api.chat_service_api.model.rest.mesage.SendMessageRequestDTO;
import com.example.domain.api.chat_service_api.service.*;
import com.example.domain.api.chat_service_api.model.dto.MessageDto;
import com.example.domain.api.statistics_module.metrics.service.IChatMetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
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
    private final IChatMetricsService chatMetricsService;
    private final ApplicationEventPublisher eventPublisher;

    private static final int MAX_CONTENT_LENGTH = 255;
    private static final String TRUNCATE_INDICATOR = "...";

    @Transactional
    public void processTelegram(TelegramResponse telegramResponse) {
        log.info("Processing incoming Telegram message from bot: {}, user: {}", telegramResponse.getBotUsername(), telegramResponse.getUsername());
        CompanyTelegramConfiguration configuration =
                telegramConfigurationRepository.findByBotUsername(telegramResponse.getBotUsername())
                        .orElseThrow(() -> {
                            log.error("Telegram configuration not found for bot: {}", telegramResponse.getBotUsername());
                            return new ResourceNotFoundException("Telegram configuration not found for bot: " + telegramResponse.getBotUsername());
                        });

        Company companyForThisBot  = configuration.getCompany();
        String telegramUsername = telegramResponse.getUsername();
        String companyIdStr = companyForThisBot .getId() != null ? companyForThisBot .getId().toString() : "unknown_company";

        Client client = clientService.findByNameAndCompanyId(telegramUsername, companyForThisBot.getId())
                .orElseGet(() -> {
                    log.info("Client with Telegram username '{}' not found for company ID {}. Creating new client.", telegramUsername, companyForThisBot .getId());
                    return clientService.createClient(telegramUsername, companyForThisBot .getId(), null);
                });
        log.debug("Using client ID {} for Telegram user '{}'", client.getId(), telegramUsername);

        Optional<Chat> existingOpenChat = chatService.findOpenChatByClientAndChannel(client.getId(), ChatChannel.Telegram);

        if (existingOpenChat.isPresent()) {
            Chat chat = existingOpenChat.get();
            log.info("Found existing open Telegram chat ID {} for client ID {}", chat.getId(), client.getId());

            SendMessageRequestDTO messageRequest = new SendMessageRequestDTO();
            messageRequest.setChatId(chat.getId());
            messageRequest.setContent(telegramResponse.getText());
            messageRequest.setSenderId(client.getId());
            messageRequest.setSenderType(ChatMessageSenderType.CLIENT);

            MessageDto messageDto = chatMessageService.processAndSaveMessage(
                    messageRequest,
                    messageRequest.getSenderId(),
                    messageRequest.getSenderType()
            );

            if (containsOperatorRequest(telegramResponse.getText())) {
                log.info("Operator request detected in Telegram chat {}", chat.getId());
                assignOperatorToChat(chat);
            } else {
                processAutoResponder(chat, messageDto);
            }
        } else {
            log.info("No existing open Telegram chat found for client ID {}. Creating new chat.", client.getId());
            CreateChatRequestDTO createChatRequest = new CreateChatRequestDTO();
            createChatRequest.setClientId(client.getId());
            createChatRequest.setCompanyId(companyForThisBot .getId());
            createChatRequest.setChatChannel(ChatChannel.Telegram);
            createChatRequest.setInitialMessageContent(telegramResponse.getText());

            try {
                ChatDetailsDTO createdChatDetails = chatService.createChat(createChatRequest);

                if (createdChatDetails.getCompanyId() != null && createdChatDetails.getChatChannel() != null) {
                    chatMetricsService.incrementChatsCreated(
                        createdChatDetails.getCompanyId().toString(),
                        createdChatDetails.getChatChannel(),
                        false
                    );
                } else {
                    log.warn("Could not explicitly increment chats_created_total metric for new Telegram chat: createdChatDetails or its fields are null.");
                }


            } catch (Exception e) {
                log.error("Failed to create Telegram chat for client ID {}: {}", client.getId(), e.getMessage(), e);
                chatMetricsService.incrementChatOperationError(
                        "ClientCompanyProcessService.processTelegram.createChat",
                        companyIdStr,
                        e.getClass().getSimpleName()
                );
            }
        }
    }

    @Transactional
    public void processEmail(String companyEmailAddress, EmailResponse emailResponse) {
        log.info("Processing incoming email for account: {}", companyEmailAddress);
        CompanyMailConfiguration mailConfiguration =
                gmailConfigurationRepository.findByEmailAddress(companyEmailAddress)
                        .orElseThrow(() -> {
                            log.error("Email configuration not found for address: {}", companyEmailAddress);
                            return new ResourceNotFoundException("Email configuration not found for address: " + companyEmailAddress);
                        });

        Company company = mailConfiguration.getCompany();
        String clientEmail = emailResponse.getFrom();
        String companyIdStr = company.getId() != null ? company.getId().toString() : "unknown_company";

        Client client = clientService.findByName(clientEmail)
                .orElseGet(() -> {
                    log.info("Client with email '{}' not found for company ID {}. Creating new client.", clientEmail, company.getId());
                    return clientService.createClient(clientEmail, company.getId(), null);
                });
        log.debug("Using client ID {} for email '{}'", client.getId(), clientEmail);

        Optional<Chat> existingOpenChat = chatService.findOpenChatByClientAndChannel(client.getId(), ChatChannel.Email);
        String fullEmailContent = formatEmailContent(emailResponse);

        if (existingOpenChat.isPresent()) {
            Chat chat = existingOpenChat.get();
            log.info("Found existing open Email chat ID {} for client ID {}", chat.getId(), client.getId());

            SendMessageRequestDTO messageRequest = new SendMessageRequestDTO();
            messageRequest.setChatId(chat.getId());
            messageRequest.setContent(truncateContent(fullEmailContent, MAX_CONTENT_LENGTH, TRUNCATE_INDICATOR));
            messageRequest.setSenderId(client.getId());
            messageRequest.setSenderType(ChatMessageSenderType.CLIENT);

            MessageDto messageDto = chatMessageService.processAndSaveMessage(messageRequest,
                    messageRequest.getSenderId(),
                    messageRequest.getSenderType());

            if (containsOperatorRequest(emailResponse.getContent())) {
                log.info("Operator request detected in Email chat {}", chat.getId());
                assignOperatorToChat(chat);
            } else {
                processAutoResponder(chat, messageDto);
            }
            log.info("Processed incoming Email message for chat ID {}. Message content snippet: {}", chat.getId(), truncateContent(messageDto.getContent(), 50, "..."));
        } else {
            log.info("No existing open Email chat found for client ID {}. Creating new chat.", client.getId());
            CreateChatRequestDTO createChatRequest = new CreateChatRequestDTO();
            createChatRequest.setClientId(client.getId());
            createChatRequest.setCompanyId(company.getId());
            createChatRequest.setChatChannel(ChatChannel.Email);
            createChatRequest.setInitialMessageContent(truncateContent(fullEmailContent, MAX_CONTENT_LENGTH, TRUNCATE_INDICATOR));

            try {
                log.debug("Attempting to create new Email chat with request: {}", createChatRequest);
                ChatDetailsDTO createdChatDetails = chatService.createChat(createChatRequest);

                if (createdChatDetails != null && createdChatDetails.getCompanyId() != null && createdChatDetails.getChatChannel() != null) {
                    chatMetricsService.incrementChatsCreated(
                        createdChatDetails.getCompanyId().toString(),
                        createdChatDetails.getChatChannel(),
                        false
                    );
                } else {
                    log.warn("Could not explicitly increment chats_created_total metric for new Email chat: createdChatDetails or its fields are null.");
                }


            } catch (Exception e) {
                log.error("Failed to create Email chat for client ID {}: {}", client.getId(), e.getMessage(), e);
                chatMetricsService.incrementChatOperationError(
                        "ClientCompanyProcessService.processEmail.createChat",
                        companyIdStr,
                        e.getClass().getSimpleName()
                );
            }
        }
    }

    @Transactional
    public void processVk(VkResponse vkResponse) {
        log.info("Processing incoming VK message from community: {}, user: {}", vkResponse.getCommunityId(), vkResponse.getFromId());
        CompanyVkConfiguration configuration =
                vkConfigurationRepository.findByCommunityId(vkResponse.getCommunityId())
                        .orElseThrow(() -> {
                            log.error("VK configuration not found for community ID: {}", vkResponse.getCommunityId());
                            return new ResourceNotFoundException("VK configuration not found for community ID: " + vkResponse.getCommunityId());
                        });

        Company company = configuration.getCompany();
        Long vkUserId = vkResponse.getFromId();
        String companyIdStr = company.getId() != null ? company.getId().toString() : "unknown_company";

        Client client = clientService.findByName(vkUserId.toString())
                .orElseGet(() -> {
                    log.info("Client with VK User ID '{}' not found for company ID {}. Creating new client.", vkUserId, company.getId());
                    return clientService.createClient(vkUserId.toString(), company.getId(), null);
                });
        log.debug("Using client ID {} for VK User ID '{}'", client.getId(), vkUserId);

        Optional<Chat> existingOpenChat = chatService.findOpenChatByClientAndChannelAndExternalId(
                client.getId(), ChatChannel.VK, vkResponse.getPeerId().toString());

        String fullMessageContent = vkResponse.getText();

        if (existingOpenChat.isPresent()) {
            Chat chat = existingOpenChat.get();
            log.info("Found existing open VK chat ID {} (external ID {}) for client ID {}", chat.getId(), chat.getExternalChatId(), client.getId());

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

            if (containsOperatorRequest(vkResponse.getText())) {
                log.info("Operator request detected in VK chat {}", chat.getId());
                assignOperatorToChat(chat);
            } else {
                processAutoResponder(chat, messageDto);
            }
            log.info("Processed incoming VK message for chat ID {}. Message content snippet: {}", chat.getId(), truncateContent(messageDto.getContent(), 50, "..."));
        } else {
            log.info("No existing open VK chat found for client ID {} and external ID {}. Creating new chat.", client.getId(), vkResponse.getPeerId().toString());
            CreateChatRequestDTO createChatRequest = new CreateChatRequestDTO();
            createChatRequest.setClientId(client.getId());
            createChatRequest.setCompanyId(company.getId());
            createChatRequest.setChatChannel(ChatChannel.VK);
            createChatRequest.setInitialMessageContent(truncateContent(fullMessageContent, MAX_CONTENT_LENGTH, TRUNCATE_INDICATOR));
            createChatRequest.setExternalChatId(vkResponse.getPeerId().toString());

            try {
                ChatDetailsDTO createdChatDetails = chatService.createChat(createChatRequest);

                if (createdChatDetails != null && createdChatDetails.getCompanyId() != null && createdChatDetails.getChatChannel() != null) {
                    chatMetricsService.incrementChatsCreated(
                        createdChatDetails.getCompanyId().toString(),
                        createdChatDetails.getChatChannel(),
                        false
                    );
                } else {
                    log.warn("Could not explicitly increment chats_created_total metric for new VK chat: createdChatDetails or its fields are null.");
                }

            } catch (Exception e) {
                log.error("Failed to create VK chat for client ID {}: {}", client.getId(), e.getMessage(), e);
                chatMetricsService.incrementChatOperationError(
                        "ClientCompanyProcessService.processVk.createChat",
                        companyIdStr,
                        e.getClass().getSimpleName()
                );
            }
        }
    }

    @Transactional
    public void processWhatsapp(WhatsappResponse whatsappResponse) {
        log.info("Processing incoming WhatsApp message from phone {} to phone ID {}",
                whatsappResponse.getFromPhoneNumber(), whatsappResponse.getRecipientPhoneNumberId());

        CompanyWhatsappConfiguration configuration =
                whatsappConfigurationRepository.findByPhoneNumberId(whatsappResponse.getRecipientPhoneNumberId())
                        .orElseThrow(() -> {
                            log.error("WhatsApp configuration not found for phone number ID: {}", whatsappResponse.getRecipientPhoneNumberId());
                            return new ResourceNotFoundException("WhatsApp configuration not found for phone number ID: " + whatsappResponse.getRecipientPhoneNumberId());
                        });

        Company company = configuration.getCompany();
        String fromPhoneNumber = whatsappResponse.getFromPhoneNumber();
        String companyIdStr = company.getId() != null ? company.getId().toString() : "unknown_company";

        Client client = clientService.findByName(fromPhoneNumber)
                .orElseGet(() -> {
                    log.info("Client with WhatsApp phone '{}' not found for company ID {}. Creating new client.", fromPhoneNumber, company.getId());
                    return clientService.createClient(fromPhoneNumber, company.getId(), null);
                });
        log.debug("Using client ID {} for WhatsApp phone '{}'", client.getId(), fromPhoneNumber);

        Optional<Chat> existingOpenChat = chatService.findOpenChatByClientAndChannel(client.getId(), ChatChannel.WhatsApp);
        String fullMessageContent = whatsappResponse.getText();

        if (existingOpenChat.isPresent()) {
            Chat chat = existingOpenChat.get();
            log.info("Found existing open WhatsApp chat ID {} for client ID {}", chat.getId(), client.getId());

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
            log.info("Processed incoming WhatsApp message for chat ID {}. Message content snippet: {}", chat.getId(), truncateContent(messageDto.getContent(), 50, "..."));
        } else {
            log.info("No existing open WhatsApp chat found for client ID {}. Creating new chat.", client.getId());
            CreateChatRequestDTO createChatRequest = new CreateChatRequestDTO();
            createChatRequest.setClientId(client.getId());
            createChatRequest.setCompanyId(company.getId());
            createChatRequest.setChatChannel(ChatChannel.WhatsApp);
            createChatRequest.setInitialMessageContent(truncateContent(fullMessageContent, MAX_CONTENT_LENGTH, TRUNCATE_INDICATOR));

            try {
                ChatDetailsDTO createdChatDetails = chatService.createChat(createChatRequest);

                if (createdChatDetails != null && createdChatDetails.getCompanyId() != null && createdChatDetails.getChatChannel() != null) {
                    chatMetricsService.incrementChatsCreated(
                        createdChatDetails.getCompanyId().toString(),
                        createdChatDetails.getChatChannel(),
                        false
                    );
                } else {
                    log.warn("Could not explicitly increment chats_created_total metric for new WhatsApp chat: createdChatDetails or its fields are null.");
                }

            } catch (Exception e) {
                log.error("Failed to create WhatsApp chat for client ID {}: {}", client.getId(), e.getMessage(), e);
                chatMetricsService.incrementChatOperationError(
                        "ClientCompanyProcessService.processWhatsapp.createChat",
                        companyIdStr,
                        e.getClass().getSimpleName()
                );
            }
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
        String companyIdStr = chat.getCompany() != null && chat.getCompany().getId() != null ? chat.getCompany().getId().toString() : "unknown";
        ChatChannel channel = chat.getChatChannel() != null ? chat.getChatChannel() : ChatChannel.UNKNOWN;
        try {
            autoResponderService.stopForChat(chat.getId());
            log.debug("Stopped AutoResponder for chat ID {} due to operator request.", chat.getId());

            Optional<User> operatorOpt = assignmentService.findLeastBusyOperator(chat.getCompany().getId());

            if (operatorOpt.isPresent()) {
                User operator = operatorOpt.get();
                chat.setUser(operator);
                chat.setStatus(ChatStatus.ASSIGNED);
                if (chat.getAssignedAt() == null) {
                    chat.setAssignedAt(LocalDateTime.now());
                }
                chatRepository.save(chat);
                log.info("Operator {} (ID: {}) assigned to chat ID {}", operator.getFullName(), operator.getId(), chat.getId());
                if (chat.getAssignedAt() != null && chat.getCreatedAt() != null) {
                    chatMetricsService.recordChatAssignmentTime(companyIdStr, channel, Duration.between(chat.getCreatedAt(), chat.getAssignedAt()));
                }
                chatMetricsService.incrementChatsAssigned(companyIdStr, channel, true);
                chatMetricsService.incrementChatsEscalated(companyIdStr, channel);

            } else {
                chat.setStatus(ChatStatus.PENDING_OPERATOR);
                chatRepository.save(chat);
                log.warn("No available operators found for company ID {}. Chat ID {} set to PENDING_OPERATOR", chat.getCompany().getId(), chat.getId());
                chatMetricsService.incrementChatsEscalated(companyIdStr, channel);
            }
        } catch (Exception e) {
            log.error("Failed to assign operator to chat ID {}: {}", chat.getId(), e.getMessage(), e);
            chatMetricsService.incrementChatOperationError(
                    "ClientCompanyProcessService.assignOperatorToChat",
                    companyIdStr,
                    e.getClass().getSimpleName()
            );
        }
    }
}
