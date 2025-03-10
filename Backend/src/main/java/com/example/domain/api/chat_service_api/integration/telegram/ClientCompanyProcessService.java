package com.example.domain.api.chat_service_api.integration.telegram;

import com.example.database.model.chats_messages_module.chat.ChatChannel;
import com.example.database.model.company_subscription_module.company.CompanyTelegramConfiguration;
import com.example.database.model.company_subscription_module.user_roles.user.User;
import com.example.database.repository.chats_messages_module.ChatMessageRepository;
import com.example.database.repository.chats_messages_module.ChatRepository;
import com.example.database.repository.chats_messages_module.CompanyTelegramConfigurationRepository;
import com.example.database.repository.crm_module.ClientRepository;
import com.example.domain.api.chat_service_api.service.ChatMessageService;
import com.example.domain.api.chat_service_api.service.ChatService;
import com.example.domain.api.company_api_test.service.ClientService;
import com.example.domain.dto.chat_module.ChatDto;
import com.example.domain.dto.chat_module.MessageDto;
import com.example.domain.dto.company_module.ClientDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ClientCompanyProcessService {

    private final CompanyTelegramConfigurationRepository telegramConfigurationRepository;
    private final ClientService clientService;
    private final ChatService chatService;
    private final ChatMessageService chatMessageService;

    private static final String chatChannelTelegram = "Telegram";

    public ClientCompanyProcessService(CompanyTelegramConfigurationRepository telegramConfigurationRepository,
                                       ClientService clientService,
                                       ChatService chatService,
                                       ChatMessageService chatMessageService) {
        this.telegramConfigurationRepository = telegramConfigurationRepository;
        this.clientService = clientService;
        this.chatService = chatService;
        this.chatMessageService = chatMessageService;
    }

    public void processTelegram(TelegramResponse telegramResponse) {

        CompanyTelegramConfiguration configuration =
                telegramConfigurationRepository.findByBotUsername(telegramResponse.getBotUsername()).get();

        //TODO продумать назначение юзера
        List<User> users = configuration.getCompany().getUsers();

        ClientDto clientDto = clientService.findByName(telegramResponse.getUsername())
                .orElseGet(() -> {
                    ClientDto newClient = new ClientDto();
                    newClient.setUserId(1);
                    newClient.setName(telegramResponse.getUsername());
                    newClient.setCreatedAt(LocalDateTime.now());
                    newClient.setUpdatedAt(LocalDateTime.now());
                    return clientService.createClient(newClient);
                });

        Integer clientId = clientService.findByNameClient(telegramResponse.getUsername())
                .orElseThrow(() -> new RuntimeException("Клиент не найден"))
                .getId();

        ChatDto chatDto = chatService.getClientAndChatChannel(clientId, "Telegram")
                .orElseGet(() -> {
                    ChatDto newChat = new ChatDto();
                    newChat.setClientId(clientId);
                    newChat.setUserId(1);
                    newChat.setChatChannel(chatChannelTelegram);
                    newChat.setStatus("ACTIVE");
                    newChat.setCreatedAt(LocalDateTime.now());
                    return chatService.createChat(newChat);
                });

        Integer chatId = chatService.findByClient(clientId)
                .orElseThrow(() -> new RuntimeException("Чат не найден"))
                .getId();

        MessageDto messageDto = new MessageDto();
        messageDto.setChatId(chatId);
        messageDto.setContent(telegramResponse.getText());
        messageDto.setSentAt(LocalDateTime.now());

        chatMessageService.createMessage(messageDto);

    }
}
