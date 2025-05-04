package com.example.domain.api.chat_service_api.service.impl;

import com.example.database.model.chats_messages_module.chat.*;
import com.example.database.model.chats_messages_module.message.ChatMessage;
import com.example.database.model.chats_messages_module.message.MessageStatus;
import com.example.database.model.company_subscription_module.company.Company;
import com.example.database.model.company_subscription_module.user_roles.user.User;
import com.example.database.model.company_subscription_module.user_roles.user.UserStatus;
import com.example.database.model.crm_module.client.Client;
import com.example.database.model.crm_module.client.TypeClient;
import com.example.database.repository.chats_messages_module.ChatMessageRepository;
import com.example.database.repository.chats_messages_module.ChatRepository;
import com.example.database.repository.company_subscription_module.UserRepository;
import com.example.database.repository.crm_module.ClientRepository;
import com.example.domain.api.chat_service_api.model.dto.MessageDto;
import com.example.domain.api.chat_service_api.model.dto.MessageStatusUpdateDTO;
import com.example.domain.api.chat_service_api.model.rest.mesage.SendMessageRequestDTO;
import com.example.domain.api.chat_service_api.service.WebSocketMessagingService;
import com.example.domain.api.chat_service_api.service.security.IChatSecurityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
@ContextConfiguration(classes = {
        TestJpaConfig.class,
        ChatMessageServiceImpl.class
})
@Transactional
class ChatMessageServiceImplIT {

    @Autowired
    private ChatMessageServiceImpl chatMessageService;

    @Autowired
    private ChatRepository chatRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private WebSocketMessagingService messagingService;

    @Autowired
    private IChatSecurityService chatSecurityService;

    private Company testCompany;
    private User testOperator;
    private Client testClient;
    private Chat testChat;

    @BeforeEach
    void setUp() {
        // Setup test company
        testCompany = new Company();
        testCompany.setName("Test Company");

        // Setup test operator
        testOperator = new User();
        testOperator.setEmail("operator@test.com");
        testOperator.setFullName("Test Operator");
        testOperator.setStatus(UserStatus.ACTIVE);
        testOperator.setCompany(testCompany);
        testOperator.setCreatedAt(LocalDateTime.now());
        testOperator = userRepository.save(testOperator);

        // Setup test client
        testClient = new Client();
        testClient.setName("Test Client");
        testClient.setTypeClient(TypeClient.IMPORTANT);
        testClient.setCompany(testCompany);
        testClient.setCreatedAt(LocalDateTime.now());
        testClient.setUpdatedAt(LocalDateTime.now());
        testClient = clientRepository.save(testClient);

        // Setup test chat
        testChat = new Chat();
        testChat.setClient(testClient);
        testChat.setChatChannel(ChatChannel.Telegram);
        testChat.setStatus(ChatStatus.ASSIGNED);
        testChat.setUser(testOperator);
        testChat.setCreatedAt(LocalDateTime.now());
        testChat = chatRepository.save(testChat);
    }

    @Test
    void processAndSaveMessage_OperatorMessage_SavesCorrectly() {
        // Given
        SendMessageRequestDTO request = new SendMessageRequestDTO();
        request.setChatId(testChat.getId());
        request.setContent("Test message");
        request.setSenderType(ChatMessageSenderType.OPERATOR);

        // When
        MessageDto result = chatMessageService.processAndSaveMessage(request, testOperator.getId(), ChatMessageSenderType.OPERATOR);

        // Then
        assertNotNull(result);
        assertNotNull(result.getId());

        // Verify database
        Optional<ChatMessage> savedMessage = chatMessageRepository.findById(result.getId());
        assertTrue(savedMessage.isPresent());
        assertEquals("Test message", savedMessage.get().getContent());
        assertEquals(testOperator.getId(), savedMessage.get().getSenderOperator().getId());
        assertEquals(testChat.getId(), savedMessage.get().getChat().getId());

        // Verify chat last message time was updated
        Chat updatedChat = chatRepository.findById(testChat.getId()).orElseThrow();
        assertNotNull(updatedChat.getLastMessageAt());

        // Verify WebSocket notification
        verify(messagingService).sendMessage(
                eq("/topic/chat/" + testChat.getId() + "/messages"),
                any(MessageDto.class)
        );
    }

    @Test
    void processAndSaveMessage_ClientMessage_SavesCorrectly() {
        // Given
        SendMessageRequestDTO request = new SendMessageRequestDTO();
        request.setChatId(testChat.getId());
        request.setContent("Client message");
        request.setSenderType(ChatMessageSenderType.CLIENT);

        // When
        MessageDto result = chatMessageService.processAndSaveMessage(request, testClient.getId(), ChatMessageSenderType.CLIENT);

        // Then
        assertNotNull(result);

        // Verify database
        ChatMessage savedMessage = chatMessageRepository.findById(result.getId()).orElseThrow();
        assertEquals("Client message", savedMessage.getContent());
        assertEquals(testClient.getId(), savedMessage.getSenderClient().getId());
        assertEquals(MessageStatus.SENT, savedMessage.getStatus());
    }

    @Test
    void getMessagesByChatId_ReturnsMessagesInCorrectOrder() {
        // Given
        ChatMessage message1 = createTestMessage("First", testOperator, ChatMessageSenderType.OPERATOR);
        ChatMessage message2 = createTestMessage("Second", testOperator, ChatMessageSenderType.OPERATOR);

        // When
        List<MessageDto> messages = chatMessageService.getMessagesByChatId(testChat.getId());

        // Then
        assertEquals(2, messages.size());
        assertEquals("First", messages.get(0).getContent());
        assertEquals("Second", messages.get(1).getContent());
    }

    @Test
    void updateMessageStatus_UpdatesStatusCorrectly() {
        // Given
        ChatMessage message = createTestMessage("Test", testOperator, ChatMessageSenderType.OPERATOR);

        // When
        MessageDto result = chatMessageService.updateMessageStatus(message.getId(), MessageStatus.READ);

        // Then
        assertEquals(MessageStatus.READ, result.getStatus());

        // Verify database
        ChatMessage updatedMessage = chatMessageRepository.findById(message.getId()).orElseThrow();
        assertEquals(MessageStatus.READ, updatedMessage.getStatus());

        // Verify WebSocket notification
        verify(messagingService).sendMessage(
                eq("/topic/chat/" + testChat.getId() + "/messages"),
                any(MessageStatusUpdateDTO.class)
        );
    }

    @Test
    void markClientMessagesAsRead_UpdatesMultipleMessages() {
        // Given
        ChatMessage message1 = createTestMessage("Msg1", testClient, ChatMessageSenderType.CLIENT);
        ChatMessage message2 = createTestMessage("Msg2", testClient, ChatMessageSenderType.CLIENT);

        // When
        int updatedCount = chatMessageService.markClientMessagesAsRead(
                testChat.getId(),
                testOperator.getId(),
                List.of(message1.getId(), message2.getId())
        );

        // Then
        assertEquals(2, updatedCount);

        // Verify database
        ChatMessage msg1 = chatMessageRepository.findById(message1.getId()).orElseThrow();
        ChatMessage msg2 = chatMessageRepository.findById(message2.getId()).orElseThrow();
        assertEquals(MessageStatus.READ, msg1.getStatus());
        assertEquals(MessageStatus.READ, msg2.getStatus());

        // Verify WebSocket notifications
        verify(messagingService, times(2)).sendMessage(
                eq("/topic/chat/" + testChat.getId() + "/messages"),
                any(MessageStatusUpdateDTO.class)
        );
    }

    @Test
    void findOpenChatByClientAndChannel_ReturnsCorrectChat() {
        // Given - testChat is already created in setUp()

        // When
        Optional<Chat> foundChat = chatMessageService.findOpenChatByClientAndChannel(
                testClient.getId(),
                ChatChannel.Telegram
        );

        // Then
        assertTrue(foundChat.isPresent());
        assertEquals(testChat.getId(), foundChat.get().getId());
    }

    private ChatMessage createTestMessage(String content, Object sender, ChatMessageSenderType senderType) {
        ChatMessage message = new ChatMessage();
        message.setChat(testChat);
        message.setContent(content);
        message.setSenderType(senderType);
        message.setStatus(MessageStatus.SENT);
        message.setSentAt(LocalDateTime.now());

        if (sender instanceof User) {
            message.setSenderOperator((User) sender);
        } else if (sender instanceof Client) {
            message.setSenderClient((Client) sender);
        }

        return chatMessageRepository.save(message);
    }
}