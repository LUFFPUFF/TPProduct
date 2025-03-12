package com.example.domain.api.chat_service_api.integration.process_service;

import com.example.database.model.chats_messages_module.chat.ChatChannel;
import com.example.database.model.company_subscription_module.company.CompanyGmailConfiguration;
import com.example.database.model.company_subscription_module.company.CompanyTelegramConfiguration;
import com.example.database.model.company_subscription_module.user_roles.user.User;
import com.example.database.repository.company_subscription_module.CompanyGmailConfigurationRepository;
import com.example.database.repository.company_subscription_module.CompanyTelegramConfigurationRepository;
import com.example.database.repository.company_subscription_module.UserRepository;
import com.example.domain.api.chat_service_api.integration.mail.email.EmailResponse;
import com.example.domain.api.chat_service_api.integration.telegram.TelegramResponse;
import com.example.domain.api.chat_service_api.service.ChatMessageService;
import com.example.domain.api.chat_service_api.service.ChatService;
import com.example.domain.api.company_api_test.service.ClientService;
import com.example.domain.dto.chat_module.ChatDto;
import com.example.domain.dto.chat_module.MessageDto;
import com.example.domain.dto.company_module.ClientDto;
import com.example.domain.dto.company_module.UserDto;
import com.example.domain.dto.mapper.MapperDto;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class ClientCompanyProcessService {

    private final CompanyTelegramConfigurationRepository telegramConfigurationRepository;
    private final CompanyGmailConfigurationRepository gmailConfigurationRepository;
    private final UserRepository userRepository;
    private final ClientService clientService;
    private final ChatService chatService;
    private final ChatMessageService chatMessageService;
    private final MapperDto mapperDto;

    private static final String chatChannelTelegram = "Telegram";

    public ClientCompanyProcessService(CompanyTelegramConfigurationRepository telegramConfigurationRepository, CompanyGmailConfigurationRepository gmailConfigurationRepository,
                                       UserRepository userRepository,
                                       ClientService clientService,
                                       ChatService chatService,
                                       ChatMessageService chatMessageService, MapperDto mapperDto) {
        this.telegramConfigurationRepository = telegramConfigurationRepository;
        this.gmailConfigurationRepository = gmailConfigurationRepository;
        this.userRepository = userRepository;
        this.clientService = clientService;
        this.chatService = chatService;
        this.chatMessageService = chatMessageService;
        this.mapperDto = mapperDto;
    }

    public void processTelegram(TelegramResponse telegramResponse) {

        CompanyTelegramConfiguration configuration =
                telegramConfigurationRepository.findByBotUsername(telegramResponse.getBotUsername()).get();

        System.out.println(configuration);

        User leastBusyUser = userRepository.findLeastBusyUser(configuration.getCompany().getId())
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        UserDto userDto = mapperDto.toDtoUser(leastBusyUser);

        System.out.println("Привязанный оператор");
        System.out.println(userDto);


        String username = telegramResponse.getUsername();
        System.out.println("Поиск клиента с именем: " + username);

        ClientDto clientDto = clientService.findByName(username)
                .orElseGet(() -> {
                    System.out.println("Клиент " + username + " не найден, создаем нового");

                    ClientDto newClient = new ClientDto();
                    newClient.setName(username);
                    newClient.setCreatedAt(LocalDateTime.now());
                    newClient.setUpdatedAt(LocalDateTime.now());
                    newClient.setUserDto(mapperDto.toDtoUser(leastBusyUser));

                    return clientService.createClient(newClient);
                });

        System.out.println("Найденный клиент: ");
        System.out.println(clientDto);


        ChatDto chatDto = chatService.getClientAndChatChannel(mapperDto.toEntityClient(clientDto), ChatChannel.Telegram)
                .orElseGet(() -> {
                    ChatDto newChat = new ChatDto();
                    newChat.setClientDto(clientDto);
                    newChat.setUserDto(userDto);
                    newChat.setChatChannel(ChatChannel.Telegram);
                    newChat.setStatus("ACTIVE");
                    newChat.setCreatedAt(LocalDateTime.now());
                    return chatService.createChat(newChat);
                });

        System.out.println("Созданный чат: ");

        System.out.println(chatDto);

        MessageDto messageDto = new MessageDto();
        messageDto.setChatDto(chatDto);
        messageDto.setContent(telegramResponse.getText());
        messageDto.setSentAt(LocalDateTime.now());
        chatMessageService.createMessage(messageDto);

        System.out.println(messageDto);

    }

    public void processGmail(String companyGmailUsername, EmailResponse emailResponse) {
        CompanyGmailConfiguration gmailConfiguration =
                gmailConfigurationRepository.findByEmailAddress(companyGmailUsername)
                        .orElseThrow(() -> new RuntimeException("Gmail конфигурация не найдена"));

        System.out.println(gmailConfiguration);

        User leastBusyUser = userRepository.findLeastBusyUser(gmailConfiguration.getCompany().getId())
                        .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        UserDto userDto = mapperDto.toDtoUser(leastBusyUser);

        System.out.println("Привязанный оператор");
        System.out.println(userDto);

        String clientEmail = emailResponse.getFrom();

        System.out.println("Поиск клиента с почтовым адресом: " + clientEmail);

        ClientDto clientDto = clientService.findByName(clientEmail)
                .orElseGet(() -> {
                    System.out.println("Клиент с почтой " + clientEmail + " не найден, создаем нового");

                    ClientDto newClient = new ClientDto();
                    newClient.setName(clientEmail);
                    newClient.setCreatedAt(LocalDateTime.now());
                    newClient.setUpdatedAt(LocalDateTime.now());
                    newClient.setUserDto(userDto);
                    return clientService.createClient(newClient);
                });

        System.out.println("Найденный клиент: ");
        System.out.println(clientDto);

        ChatDto chatDto = chatService.getClientAndChatChannel(mapperDto.toEntityClient(clientDto), ChatChannel.Email)
                .orElseGet(() -> {
                    ChatDto newChat = new ChatDto();
                    newChat.setClientDto(clientDto);
                    newChat.setUserDto(userDto);
                    newChat.setChatChannel(ChatChannel.Email);
                    newChat.setStatus("ACTIVE");
                    newChat.setCreatedAt(LocalDateTime.now());
                    return chatService.createChat(newChat);
                });

        System.out.println("Созданный чат: ");
        System.out.println(chatDto);


        MessageDto messageDto = new MessageDto();
        messageDto.setChatDto(chatDto);
        messageDto.setContent(emailResponse.getContent());
        messageDto.setSentAt(LocalDateTime.now());
        chatMessageService.createMessage(messageDto);

        System.out.println(messageDto);
    }
}
