package com.example.domain.api.chat_service_api.integration.process_service;

import com.example.database.model.chats_messages_module.chat.Chat;
import com.example.database.model.chats_messages_module.chat.ChatChannel;
import com.example.database.model.chats_messages_module.chat.ChatMessageSenderType;
import com.example.database.model.company_subscription_module.company.Company;
import com.example.database.model.company_subscription_module.company.CompanyMailConfiguration;
import com.example.database.model.company_subscription_module.company.CompanyTelegramConfiguration;
import com.example.database.model.company_subscription_module.user_roles.user.User;
import com.example.database.model.crm_module.client.Client;
import com.example.database.repository.company_subscription_module.CompanyMailConfigurationRepository;
import com.example.database.repository.company_subscription_module.CompanyTelegramConfigurationRepository;
import com.example.domain.api.chat_service_api.exception_handler.ResourceNotFoundException;
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
    private final IAssignmentService assignmentService;

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

            MessageDto messageDto = chatMessageService.processAndSaveMessage(messageRequest, messageRequest.getSenderId(), messageRequest.getSenderType());
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
            SendMessageRequestDTO messageRequest = new SendMessageRequestDTO();
            messageRequest.setChatId(existingOpenChat.get().getId());
            messageRequest.setContent("Subject: " + emailResponse.getSubject() + "\n\n" + emailResponse.getContent());
            messageRequest.setSenderId(client.getId());
            messageRequest.setSenderType(ChatMessageSenderType.CLIENT);

            chatMessageService.processAndSaveMessage(messageRequest, messageRequest.getSenderId(), messageRequest.getSenderType());
        } else {
            CreateChatRequestDTO createChatRequest = new CreateChatRequestDTO();
            createChatRequest.setClientId(client.getId());
            createChatRequest.setCompanyId(company.getId());
            createChatRequest.setChatChannel(ChatChannel.Email);
            createChatRequest.setInitialMessageContent("Subject: " + emailResponse.getSubject() + "\n\n" + emailResponse.getContent());

            chatService.createChat(createChatRequest);
        }
    }
}
