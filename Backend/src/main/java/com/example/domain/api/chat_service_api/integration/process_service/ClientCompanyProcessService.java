package com.example.domain.api.chat_service_api.integration.process_service;

import com.example.database.model.chats_messages_module.chat.Chat;
import com.example.database.model.chats_messages_module.chat.ChatChannel;
import com.example.database.model.chats_messages_module.chat.ChatMessageSenderType;
import com.example.database.model.chats_messages_module.chat.ChatStatus;
import com.example.database.model.company_subscription_module.company.Company;
import com.example.database.model.company_subscription_module.company.CompanyMailConfiguration;
import com.example.database.model.company_subscription_module.company.CompanyTelegramConfiguration;
import com.example.database.model.company_subscription_module.user_roles.user.User;
import com.example.database.model.crm_module.client.Client;
import com.example.database.repository.chats_messages_module.ChatRepository;
import com.example.database.repository.company_subscription_module.CompanyMailConfigurationRepository;
import com.example.database.repository.company_subscription_module.CompanyTelegramConfigurationRepository;
import com.example.domain.api.ans_api_module.exception.AutoResponderException;
import com.example.domain.api.ans_api_module.service.IAutoResponderService;
import com.example.domain.api.chat_service_api.exception_handler.ResourceNotFoundException;
import com.example.domain.api.chat_service_api.exception_handler.exception.service.ChatServiceException;
import com.example.domain.api.chat_service_api.integration.mail.response.EmailResponse;
import com.example.domain.api.chat_service_api.integration.telegram.TelegramResponse;
import com.example.domain.api.chat_service_api.model.dto.ChatDetailsDTO;
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
    private final IClientService clientService;
    private final IChatService chatService;
    private final IChatMessageService chatMessageService;
    private final IAutoResponderService autoResponderService;
    private final IAssignmentService assignmentService;
    private final ChatRepository chatRepository;

    @Transactional
    public void processTelegram(TelegramResponse telegramResponse) {
        CompanyTelegramConfiguration configuration =
                telegramConfigurationRepository.findByBotUsername(telegramResponse.getBotUsername())
                        .orElseThrow(() -> new ResourceNotFoundException("Telegram configuration not found"));

        Company company = configuration.getCompany();
        String telegramUsername = telegramResponse.getBotUsername();

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
    public void processEmail(String companyGmailUsername, EmailResponse emailResponse) {
        CompanyMailConfiguration gmailConfiguration =
                gmailConfigurationRepository.findByEmailAddress(companyGmailUsername)
                        .orElseThrow(() -> new ResourceNotFoundException("Gmail configuration not found"));

        Company company = gmailConfiguration.getCompany();
        String clientEmail = emailResponse.getFrom();

        Client client = clientService.findByName(clientEmail)
                .orElseGet(() -> clientService.createClient(clientEmail, company.getId(), null));

        Optional<Chat> existingOpenChat = chatService.findOpenChatByClientAndChannel(client.getId(), ChatChannel.Email);

        if (existingOpenChat.isPresent()) {

            Chat chat = existingOpenChat.get();

            SendMessageRequestDTO messageRequest = new SendMessageRequestDTO();
            messageRequest.setChatId(existingOpenChat.get().getId());
            messageRequest.setContent("Subject: " + emailResponse.getSubject() + "\n\n" + emailResponse.getContent());
            messageRequest.setSenderId(client.getId());
            messageRequest.setSenderType(ChatMessageSenderType.CLIENT);

            MessageDto messageDto = chatMessageService.processAndSaveMessage(messageRequest,
                    messageRequest.getSenderId(),
                    messageRequest.getSenderType());

            if (containsOperatorRequest(emailResponse.getContent())) {
                log.info("Operator request detected in chat {}", chat.getId());
                assignOperatorToChat(chat);
            } else {
                processAutoResponder(chat, messageDto);
            }

        } else {
            CreateChatRequestDTO createChatRequest = new CreateChatRequestDTO();
            createChatRequest.setClientId(client.getId());
            createChatRequest.setCompanyId(company.getId());
            createChatRequest.setChatChannel(ChatChannel.Email);
            createChatRequest.setInitialMessageContent(formatEmailContent(emailResponse));

            chatService.createChat(createChatRequest);
        }
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
