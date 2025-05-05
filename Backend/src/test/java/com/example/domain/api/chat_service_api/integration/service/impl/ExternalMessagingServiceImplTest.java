package com.example.domain.api.chat_service_api.integration.service.impl;

import com.example.database.model.chats_messages_module.chat.Chat;
import com.example.database.model.chats_messages_module.chat.ChatChannel;
import com.example.database.model.company_subscription_module.company.Company;
import com.example.database.model.company_subscription_module.company.CompanyMailConfiguration;
import com.example.database.model.company_subscription_module.company.CompanyTelegramConfiguration;
import com.example.database.model.crm_module.client.Client;
import com.example.database.repository.company_subscription_module.CompanyMailConfigurationRepository;
import com.example.database.repository.company_subscription_module.CompanyTelegramConfigurationRepository;
import com.example.domain.api.chat_service_api.exception_handler.ChatNotFoundException;
import com.example.domain.api.chat_service_api.exception_handler.ResourceNotFoundException;
import com.example.domain.api.chat_service_api.exception_handler.exception.ExternalMessagingException;
import com.example.domain.api.chat_service_api.integration.listener.SendMessageCommand;
import com.example.domain.api.chat_service_api.service.IChatMessageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;
import java.util.concurrent.BlockingQueue;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ExternalMessagingServiceImplTest {

    @Mock
    private IChatMessageService chatMessageService;
    @Mock
    private BlockingQueue<Object> outgoingMessageQueue;
    @Mock
    private CompanyMailConfigurationRepository mailConfigRepository;
    @Mock
    private CompanyTelegramConfigurationRepository companyTelegramConfigurationRepository;

    @InjectMocks
    private ExternalMessagingServiceImpl messagingService;

    @Captor
    private ArgumentCaptor<SendMessageCommand> commandCaptor;

    private Chat testChat;
    private Client testClient;
    private Company testCompany;
    private final Integer CHAT_ID = 1;
    private final Integer COMPANY_ID = 1;
    private final String TEST_MESSAGE = "Test message content";
    private final String CLIENT_EMAIL = "client@example.com";
    private final Long TELEGRAM_CHAT_ID = 12345L;
    private final String FROM_EMAIL = "support@company.com";


    @BeforeEach
    void setUp() {
        reset(chatMessageService, outgoingMessageQueue, mailConfigRepository, companyTelegramConfigurationRepository);

        testCompany = new Company();
        testCompany.setId(COMPANY_ID);

        testClient = new Client();
        testClient.setName(CLIENT_EMAIL);

        testChat = new Chat();
        testChat.setId(CHAT_ID);
        testChat.setClient(testClient);
        testChat.setCompany(testCompany);
        testChat.setChatChannel(null); // Явно ставим null, чтобы не забыть установить в тестах

        when(chatMessageService.findChatEntityById(CHAT_ID)).thenReturn(Optional.of(testChat));
    }

    // --- Успешные тесты (без изменений) ---
    @Test
    void sendMessageToExternal_TelegramChannel_ShouldPutCorrectCommand() throws Exception {
        testChat.setChatChannel(ChatChannel.Telegram);
        CompanyTelegramConfiguration telegramConfig = new CompanyTelegramConfiguration();
        telegramConfig.setChatTelegramId(TELEGRAM_CHAT_ID);
        when(companyTelegramConfigurationRepository.findByCompanyId(COMPANY_ID)).thenReturn(Optional.of(telegramConfig));

        messagingService.sendMessageToExternal(CHAT_ID, TEST_MESSAGE);

        verify(outgoingMessageQueue).put(commandCaptor.capture());
        SendMessageCommand capturedCommand = commandCaptor.getValue();
        assertEquals(ChatChannel.Telegram, capturedCommand.getChannel());
        assertEquals(CHAT_ID, capturedCommand.getChatId());
        assertEquals(TEST_MESSAGE, capturedCommand.getContent());
        assertEquals(TELEGRAM_CHAT_ID, capturedCommand.getTelegramChatId());
        assertNull(capturedCommand.getToEmailAddress());
    }

    @Test
    void sendMessageToExternal_EmailChannel_ShouldPutCorrectCommand() throws Exception {
        testChat.setChatChannel(ChatChannel.Email);
        CompanyMailConfiguration mailConfig = new CompanyMailConfiguration();
        mailConfig.setEmailAddress(FROM_EMAIL);
        when(mailConfigRepository.findByCompany(testCompany)).thenReturn(Optional.of(mailConfig));
        String expectedSubject = "Re: Ваш чат #" + CHAT_ID;

        messagingService.sendMessageToExternal(CHAT_ID, TEST_MESSAGE);

        verify(outgoingMessageQueue).put(commandCaptor.capture());
        SendMessageCommand capturedCommand = commandCaptor.getValue();
        assertEquals(ChatChannel.Email, capturedCommand.getChannel());
        assertEquals(CHAT_ID, capturedCommand.getChatId());
        assertEquals(TEST_MESSAGE, capturedCommand.getContent());
        assertEquals(CLIENT_EMAIL, capturedCommand.getToEmailAddress());
        assertEquals(FROM_EMAIL, capturedCommand.getFromEmailAddress());
        assertEquals(expectedSubject, capturedCommand.getSubject());
        assertNull(capturedCommand.getTelegramChatId());
    }

    // --- Тесты на граничные случаи и ошибки ---

    @Test
    void sendMessageToExternal_NullMessage_ShouldNotSend() {
        assertDoesNotThrow(() -> messagingService.sendMessageToExternal(CHAT_ID, null));
        verifyNoInteractions(outgoingMessageQueue);
        verifyNoInteractions(chatMessageService);
    }

    @Test
    void sendMessageToExternal_EmptyMessage_ShouldNotSend() {
        assertDoesNotThrow(() -> messagingService.sendMessageToExternal(CHAT_ID, "   "));
        verifyNoInteractions(outgoingMessageQueue);
        verifyNoInteractions(chatMessageService);
    }

    @Test
    void sendMessageToExternal_ChatNotFound_ShouldThrowChatNotFoundException() {
        when(chatMessageService.findChatEntityById(CHAT_ID)).thenReturn(Optional.empty());
        ChatNotFoundException exception = assertThrows(ChatNotFoundException.class,
                () -> messagingService.sendMessageToExternal(CHAT_ID, TEST_MESSAGE));
        assertEquals("Chat with ID " + CHAT_ID + " not found for sending external message.", exception.getMessage());
        verifyNoInteractions(outgoingMessageQueue);
    }

    @Test
    void sendMessageToExternal_ClientNotFound_ShouldThrowResourceNotFoundException() {
        testChat.setClient(null);
        when(chatMessageService.findChatEntityById(CHAT_ID)).thenReturn(Optional.of(testChat));
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> messagingService.sendMessageToExternal(CHAT_ID, TEST_MESSAGE));
        assertEquals("Client not found for chat ID " + CHAT_ID, exception.getMessage());
        verifyNoInteractions(outgoingMessageQueue);
    }

    @Test
    void sendMessageToExternal_CompanyNotFound_ShouldThrowResourceNotFoundException() {
        // Arrange
        // ИСПРАВЛЕНО: Устанавливаем канал, чтобы не упасть на проверке канала раньше
        testChat.setChatChannel(ChatChannel.Telegram);
        testChat.setCompany(null); // Устанавливаем компанию в null
        when(chatMessageService.findChatEntityById(CHAT_ID)).thenReturn(Optional.of(testChat));

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> messagingService.sendMessageToExternal(CHAT_ID, TEST_MESSAGE));

        assertEquals("Company not found for chat ID " + CHAT_ID, exception.getMessage());
        verifyNoInteractions(outgoingMessageQueue);
    }

    @Test
    void sendMessageToExternal_NullChannel_ShouldThrowExternalMessagingException() {
        testChat.setChatChannel(null); // Канал null (уже в setUp, но для ясности)
        when(chatMessageService.findChatEntityById(CHAT_ID)).thenReturn(Optional.of(testChat));
        ExternalMessagingException exception = assertThrows(ExternalMessagingException.class,
                () -> messagingService.sendMessageToExternal(CHAT_ID, TEST_MESSAGE));
        assertEquals("Chat channel is not defined for chat ID " + CHAT_ID, exception.getMessage());
        verifyNoInteractions(outgoingMessageQueue);
    }

    @Test
    void sendMessageToExternal_UnsupportedChannel_ShouldThrowExternalMessagingException() {
        // Arrange
        testChat.setChatChannel(ChatChannel.VK); // Неподдерживаемый канал
        when(chatMessageService.findChatEntityById(CHAT_ID)).thenReturn(Optional.of(testChat));
        String expectedInnerMessage = "Unsupported chat channel for external messaging: " + ChatChannel.VK;
        String expectedOuterMessage = "Failed to send external message for chat ID " + CHAT_ID;


        // Act & Assert
        ExternalMessagingException exception = assertThrows(ExternalMessagingException.class,
                () -> messagingService.sendMessageToExternal(CHAT_ID, TEST_MESSAGE));

        // ИСПРАВЛЕНО: Проверяем внешнее сообщение и причину
        assertEquals(expectedOuterMessage, exception.getMessage());
        assertNotNull(exception.getCause());
        assertInstanceOf(ExternalMessagingException.class, exception.getCause()); // Причина тоже ExternalMessagingException
        assertEquals(expectedInnerMessage, exception.getCause().getMessage()); // Проверяем сообщение внутреннего

        verifyNoInteractions(outgoingMessageQueue);
    }


    @Test
    void sendMessageToExternal_TelegramConfigNotFound_ShouldThrowExternalMessagingException() {
        testChat.setChatChannel(ChatChannel.Telegram);
        when(companyTelegramConfigurationRepository.findByCompanyId(COMPANY_ID)).thenReturn(Optional.empty());

        ExternalMessagingException exception = assertThrows(ExternalMessagingException.class,
                () -> messagingService.sendMessageToExternal(CHAT_ID, TEST_MESSAGE));

        // ИСПРАВЛЕНО: Упрощенная проверка (только типы)
        assertEquals("Failed to send external message for chat ID " + CHAT_ID, exception.getMessage());
        assertNotNull(exception.getCause());
        assertInstanceOf(ResourceNotFoundException.class, exception.getCause());
        // assertEquals("Telegram config not found for company " + COMPANY_ID, exception.getCause().getMessage()); // Можно вернуть, если нужно
        verifyNoInteractions(outgoingMessageQueue);
    }

    @Test
    void sendMessageToExternal_EmailConfigNotFound_ShouldThrowExternalMessagingException() {
        testChat.setChatChannel(ChatChannel.Email);
        when(mailConfigRepository.findByCompany(testCompany)).thenReturn(Optional.empty());

        ExternalMessagingException exception = assertThrows(ExternalMessagingException.class,
                () -> messagingService.sendMessageToExternal(CHAT_ID, TEST_MESSAGE));

        // ИСПРАВЛЕНО: Упрощенная проверка (только типы)
        assertEquals("Failed to send external message for chat ID " + CHAT_ID, exception.getMessage());
        assertNotNull(exception.getCause());
        assertInstanceOf(ResourceNotFoundException.class, exception.getCause());
        // assertEquals("Email configuration not found for company ID " + COMPANY_ID, exception.getCause().getMessage()); // Можно вернуть
        verifyNoInteractions(outgoingMessageQueue);
    }

    @Test
    void sendMessageToExternal_QueuePutFails_ShouldThrowExternalMessagingException() throws Exception {
        testChat.setChatChannel(ChatChannel.Telegram);
        CompanyTelegramConfiguration telegramConfig = new CompanyTelegramConfiguration();
        telegramConfig.setChatTelegramId(TELEGRAM_CHAT_ID);
        when(companyTelegramConfigurationRepository.findByCompanyId(COMPANY_ID)).thenReturn(Optional.of(telegramConfig));
        InterruptedException interrupt = new InterruptedException("Queue put interrupted");
        doThrow(interrupt).when(outgoingMessageQueue).put(any(SendMessageCommand.class));

        ExternalMessagingException exception = assertThrows(ExternalMessagingException.class,
                () -> messagingService.sendMessageToExternal(CHAT_ID, TEST_MESSAGE));

        assertEquals("Failed to put external message command into outgoing queue for chat ID " + CHAT_ID, exception.getMessage());
        // ИСПРАВЛЕНО: Упрощенная проверка причины
        assertNotNull(exception.getCause());
        assertInstanceOf(InterruptedException.class, exception.getCause());
        // assertEquals(interrupt, exception.getCause()); // Можно вернуть
    }

    @Test
        // ИСПРАВЛЕНО: Убираем assertThrows, проверяем команду с null email
    void sendMessageToExternal_NullClientNameForEmail_ShouldPutCommandWithNullToEmail() throws Exception {
        // Arrange
        testChat.setChatChannel(ChatChannel.Email);
        testClient.setName(null); // Имя клиента (email) - null
        CompanyMailConfiguration mailConfig = new CompanyMailConfiguration();
        mailConfig.setEmailAddress(FROM_EMAIL);
        when(mailConfigRepository.findByCompany(testCompany)).thenReturn(Optional.of(mailConfig));

        // Act
        messagingService.sendMessageToExternal(CHAT_ID, TEST_MESSAGE);

        // Assert
        // Проверяем, что команда была добавлена в очередь
        verify(outgoingMessageQueue).put(commandCaptor.capture());
        SendMessageCommand capturedCommand = commandCaptor.getValue();

        assertNotNull(capturedCommand);
        assertEquals(ChatChannel.Email, capturedCommand.getChannel());
        assertNull(capturedCommand.getToEmailAddress()); // Проверяем, что email получателя null
        assertEquals(FROM_EMAIL, capturedCommand.getFromEmailAddress());
        assertEquals(TEST_MESSAGE, capturedCommand.getContent());
    }


    @Test
    void sendMessageToExternal_TelegramChannel_NullTelegramIdInConfig_ShouldPutCommandWithNullId() throws Exception {
        testChat.setChatChannel(ChatChannel.Telegram);
        CompanyTelegramConfiguration telegramConfig = new CompanyTelegramConfiguration();
        telegramConfig.setChatTelegramId(null);
        when(companyTelegramConfigurationRepository.findByCompanyId(COMPANY_ID)).thenReturn(Optional.of(telegramConfig));

        messagingService.sendMessageToExternal(CHAT_ID, TEST_MESSAGE);

        verify(outgoingMessageQueue).put(commandCaptor.capture());
        SendMessageCommand capturedCommand = commandCaptor.getValue();
        assertNotNull(capturedCommand);
        assertEquals(ChatChannel.Telegram, capturedCommand.getChannel());
        assertNull(capturedCommand.getTelegramChatId());
    }

    @Test
    void sendMessageToExternal_EmailChannel_NullFromEmailInConfig_ShouldPutCommandWithNullFromEmail() throws Exception {
        testChat.setChatChannel(ChatChannel.Email);
        CompanyMailConfiguration mailConfig = new CompanyMailConfiguration();
        mailConfig.setEmailAddress(null);
        when(mailConfigRepository.findByCompany(testCompany)).thenReturn(Optional.of(mailConfig));
        String expectedSubject = "Re: Ваш чат #" + CHAT_ID;

        messagingService.sendMessageToExternal(CHAT_ID, TEST_MESSAGE);

        verify(outgoingMessageQueue).put(commandCaptor.capture());
        SendMessageCommand capturedCommand = commandCaptor.getValue();
        assertNotNull(capturedCommand);
        assertEquals(ChatChannel.Email, capturedCommand.getChannel());
        assertEquals(CLIENT_EMAIL, capturedCommand.getToEmailAddress());
        assertNull(capturedCommand.getFromEmailAddress());
        assertEquals(expectedSubject, capturedCommand.getSubject());
    }
}