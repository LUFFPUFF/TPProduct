package com.example.domain.api.chat_service_api.service.impl;

import com.example.database.model.chats_messages_module.chat.Chat;
import com.example.database.model.chats_messages_module.chat.ChatMessageSenderType;
import com.example.database.model.chats_messages_module.message.ChatMessage;
import com.example.database.model.chats_messages_module.message.MessageStatus;
import com.example.database.model.company_subscription_module.user_roles.user.User;
import com.example.database.model.crm_module.client.Client;
import com.example.database.repository.chats_messages_module.ChatMessageRepository;
import com.example.database.repository.chats_messages_module.ChatRepository;
import com.example.database.repository.company_subscription_module.UserRepository;
import com.example.database.repository.crm_module.ClientRepository;
import com.example.domain.api.chat_service_api.exception_handler.ChatNotFoundException;
import com.example.domain.api.chat_service_api.exception_handler.ResourceNotFoundException;
import com.example.domain.api.chat_service_api.mapper.ChatMessageMapper;
import com.example.domain.api.chat_service_api.model.dto.MessageDto;
import com.example.domain.api.chat_service_api.model.dto.MessageStatusUpdateDTO;
import com.example.domain.api.chat_service_api.model.rest.mesage.SendMessageRequestDTO;
import com.example.domain.api.chat_service_api.service.WebSocketMessagingService;
import com.example.domain.api.chat_service_api.service.security.IChatSecurityService;
import com.example.domain.dto.AppUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ChatMessageServiceImplTest {

    @Mock private ChatMessageRepository chatMessageRepository;
    @Mock private ChatRepository chatRepository;
    @Mock private UserRepository userRepository;
    @Mock private ClientRepository clientRepository;
    @Mock private ChatMessageMapper chatMessageMapper;
    @Mock private WebSocketMessagingService messagingService;
    @Mock private IChatSecurityService chatSecurityService;

    @InjectMocks
    private ChatMessageServiceImpl chatMessageService;

    private SendMessageRequestDTO messageRequest;
    private Chat chat;
    private User operator;
    private Client client;
    private ChatMessage message;
    private AppUserDetails operatorUser;

    @BeforeEach
    void setUp() {
        messageRequest = new SendMessageRequestDTO();
        messageRequest.setChatId(1);
        messageRequest.setContent("Test message");

        chat = new Chat();
        chat.setId(1);

        operator = new User();
        operator.setId(1);

        client = new Client();
        client.setId(1);



        message = new ChatMessage();
        message.setId(1);
        message.setChat(chat);
        message.setStatus(MessageStatus.SENT);

        operatorUser = new AppUserDetails(1, 1, "operator@test.com", "password",
                Collections.singletonList(new SimpleGrantedAuthority("OPERATOR")));
    }

    @Test
    void processAndSaveMessage_WhenOperatorSendsMessage_ShouldSaveMessage() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(operatorUser);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        messageRequest.setSenderType(ChatMessageSenderType.OPERATOR);

        when(chatRepository.findById(1)).thenReturn(Optional.of(chat));
        when(userRepository.findById(1)).thenReturn(Optional.of(operator));
        when(chatSecurityService.canProcessAndSaveMessage(1, 1, ChatMessageSenderType.OPERATOR)).thenReturn(true);
        when(chatMessageRepository.save(any())).thenReturn(message);
        when(chatMessageMapper.toDto(any())).thenReturn(new MessageDto());

        MessageDto result = chatMessageService.processAndSaveMessage(messageRequest, 1, ChatMessageSenderType.OPERATOR);

        assertNotNull(result);
        verify(chatMessageRepository).save(any());
        verify(messagingService).sendMessage(eq("/topic/chat/1/messages"), any(MessageDto.class));
    }

    @Test
    void processAndSaveMessage_WhenOperatorAccessDenied_ShouldThrowException() {
        messageRequest.setSenderType(ChatMessageSenderType.OPERATOR);

        when(chatRepository.findById(1)).thenReturn(Optional.of(chat));
        when(userRepository.findById(1)).thenReturn(Optional.of(operator));
        when(chatSecurityService.canProcessAndSaveMessage(1, 1, ChatMessageSenderType.OPERATOR)).thenReturn(false);

        // Этот мок нужен, даже если метод не должен вызваться
        when(chatMessageRepository.save(any())).thenReturn(new ChatMessage());

        assertThrows(AccessDeniedException.class, () ->
                chatMessageService.processAndSaveMessage(messageRequest, 1, ChatMessageSenderType.OPERATOR));
    }




    @Test
    void processAndSaveMessage_WhenClientSendsMessage_ShouldSaveMessage() {
        messageRequest.setSenderType(ChatMessageSenderType.CLIENT);

        when(chatRepository.findById(1)).thenReturn(Optional.of(chat));
        when(clientRepository.findById(1)).thenReturn(Optional.of(client));
        when(chatSecurityService.canProcessAndSaveMessage(1, 1, ChatMessageSenderType.CLIENT)).thenReturn(true);
        when(chatMessageRepository.save(any())).thenReturn(message);
        when(chatMessageMapper.toDto(any())).thenReturn(new MessageDto());

        MessageDto result = chatMessageService.processAndSaveMessage(messageRequest, 1, ChatMessageSenderType.CLIENT);

        assertNotNull(result);
        verify(chatMessageRepository).save(any());
        verify(messagingService).sendMessage(eq("/topic/chat/1/messages"), any(MessageDto.class));
    }

    @Test
    void processAndSaveMessage_WhenClientAccessDenied_ShouldThrowException() {
        messageRequest.setSenderType(ChatMessageSenderType.CLIENT);

        when(chatRepository.findById(1)).thenReturn(Optional.of(chat));
        when(clientRepository.findById(1)).thenReturn(Optional.of(client));
        when(chatSecurityService.canProcessAndSaveMessage(1, 1, ChatMessageSenderType.CLIENT)).thenReturn(false);

        when(chatMessageRepository.save(any())).thenReturn(new ChatMessage());

        assertThrows(AccessDeniedException.class, () ->
                chatMessageService.processAndSaveMessage(messageRequest, 1, ChatMessageSenderType.CLIENT));
    }




    @Test
    void processAndSaveMessage_WhenClientNotFound_ShouldThrowException() {
        messageRequest.setSenderType(ChatMessageSenderType.CLIENT);
        when(chatRepository.findById(1)).thenReturn(Optional.of(chat));
        when(clientRepository.findById(1)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                chatMessageService.processAndSaveMessage(messageRequest, 1, ChatMessageSenderType.CLIENT));
    }

    @Test
    void getMessagesByChatId_ShouldReturnMessages() {
        when(chatMessageRepository.findByChatIdOrderBySentAtAsc(1)).thenReturn(List.of(message));
        when(chatSecurityService.canAccessChat(1)).thenReturn(true);
        when(chatMessageMapper.toDto(any())).thenReturn(new MessageDto());

        List<MessageDto> result = chatMessageService.getMessagesByChatId(1);

        assertEquals(1, result.size());
        verify(chatMessageRepository).findByChatIdOrderBySentAtAsc(1);
    }

    @Test
    void updateMessageStatus_WhenMessageExists_ShouldUpdateStatus() {
        when(chatMessageRepository.findById(1)).thenReturn(Optional.of(message));
        when(chatSecurityService.canUpdateMessageStatus(1)).thenReturn(true);
        when(chatMessageRepository.save(any())).thenReturn(message);
        when(chatMessageMapper.toDto(any())).thenReturn(new MessageDto());

        MessageDto result = chatMessageService.updateMessageStatus(1, MessageStatus.READ);

        assertNotNull(result);
        assertEquals(MessageStatus.READ, message.getStatus());
        verify(chatMessageRepository).save(any());
        verify(messagingService).sendMessage(eq("/topic/chat/1/messages"), any(MessageStatusUpdateDTO.class));
    }

    @Test
    void updateMessageStatus_WhenAccessDenied_ShouldThrowException() {
        when(chatMessageRepository.findById(1)).thenReturn(Optional.of(message));
        when(chatSecurityService.canUpdateMessageStatus(1)).thenReturn(false);

        when(chatMessageRepository.save(any())).thenReturn(new ChatMessage());

        assertThrows(AccessDeniedException.class, () ->
                chatMessageService.updateMessageStatus(1, MessageStatus.READ));
    }




    @Test
    void processAndSaveMessage_WhenChatNotFound_ShouldThrowException() {
        messageRequest.setSenderType(ChatMessageSenderType.OPERATOR);
        when(chatRepository.findById(1)).thenReturn(Optional.empty());

        assertThrows(ChatNotFoundException.class, () ->
                chatMessageService.processAndSaveMessage(messageRequest, 1, ChatMessageSenderType.OPERATOR));
    }

    @Test
    void processAndSaveMessage_WhenOperatorNotFound_ShouldThrowException() {
        messageRequest.setSenderType(ChatMessageSenderType.OPERATOR);
        when(chatRepository.findById(1)).thenReturn(Optional.of(chat));
        when(userRepository.findById(1)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                chatMessageService.processAndSaveMessage(messageRequest, 1, ChatMessageSenderType.OPERATOR));
    }

    @Test
    void markClientMessagesAsRead_WhenValidRequest_ShouldUpdateStatus() {
        // Arrange
        List<Integer> messageIds = List.of(1, 2, 3);

        when(chatMessageRepository.markClientMessagesAsRead(eq(1), eq(MessageStatus.READ), eq(messageIds)))
                .thenReturn(3);

        // Act
        int updatedCount = chatMessageService.markClientMessagesAsRead(1, 1, messageIds);

        // Assert
        assertEquals(3, updatedCount);
        verify(messagingService, times(3)).sendMessage(eq("/topic/chat/1/messages"), any(MessageStatusUpdateDTO.class));
    }

    @Test
    void markClientMessagesAsRead_WhenEmptyList_ShouldReturnZero() {
        // Act
        int updatedCount = chatMessageService.markClientMessagesAsRead(1, 1, Collections.emptyList());

        // Assert
        assertEquals(0, updatedCount);
        verify(messagingService, never()).sendMessage(any(), any());
    }

    @Test
    void processAndSaveMessage_WhenOperatorWithCompanyAccess_ShouldSucceed() {
        // 1. Пользователь с доступом к компании
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(operatorUser);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        messageRequest.setSenderType(ChatMessageSenderType.OPERATOR);

        when(chatRepository.findById(1)).thenReturn(Optional.of(chat));
        when(userRepository.findById(1)).thenReturn(Optional.of(operator));
        when(chatSecurityService.canProcessAndSaveMessage(1, 1, ChatMessageSenderType.OPERATOR)).thenReturn(true);
        when(chatMessageRepository.save(any())).thenReturn(message);
        when(chatMessageMapper.toDto(any())).thenReturn(new MessageDto());

        MessageDto result = chatMessageService.processAndSaveMessage(messageRequest, 1, ChatMessageSenderType.OPERATOR);

        assertNotNull(result);
        verify(chatSecurityService).canProcessAndSaveMessage(1, 1, ChatMessageSenderType.OPERATOR);
    }

    @Test
    void processAndSaveMessage_WhenOperatorWithoutCompanyAccess_ShouldDenyAccess() {
        // 2. Пользователь без доступа к компании
        AppUserDetails userWithoutAccess = new AppUserDetails(2, 2, "noaccess@test.com", "password",
                Collections.singletonList(new SimpleGrantedAuthority("OPERATOR")));

        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(userWithoutAccess);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        messageRequest.setSenderType(ChatMessageSenderType.OPERATOR);

        when(chatRepository.findById(1)).thenReturn(Optional.of(chat));
        when(userRepository.findById(2)).thenReturn(Optional.of(new User()));
        when(chatSecurityService.canProcessAndSaveMessage(1, 2, ChatMessageSenderType.OPERATOR)).thenReturn(false);

        assertThrows(AccessDeniedException.class, () ->
                chatMessageService.processAndSaveMessage(messageRequest, 2, ChatMessageSenderType.OPERATOR));
    }

    @Test
    void getMessagesByChatId_WhenUserHasNoAccess_ShouldDenyAccess() {
        // 3. Проверка прав доступа
        when(chatSecurityService.canAccessChat(1)).thenReturn(false);

        assertThrows(AccessDeniedException.class, () ->
                chatMessageService.getMessagesByChatId(1));
    }


}
