package com.example.domain.api.chat_service_api.service.impl;

import com.example.database.model.chats_messages_module.chat.Chat;
import com.example.database.model.chats_messages_module.chat.ChatChannel;
import com.example.database.model.chats_messages_module.chat.ChatStatus;
import com.example.database.model.company_subscription_module.company.Company;
import com.example.database.model.company_subscription_module.user_roles.user.Role;
import com.example.database.model.company_subscription_module.user_roles.user.User;
import com.example.database.model.crm_module.client.Client;
import com.example.database.repository.chats_messages_module.ChatMessageRepository;
import com.example.database.repository.chats_messages_module.ChatRepository;
import com.example.domain.api.ans_api_module.service.IAutoResponderService;
import com.example.domain.api.chat_service_api.exception_handler.ChatNotFoundException;
import com.example.domain.api.chat_service_api.exception_handler.ResourceNotFoundException;
import com.example.domain.api.chat_service_api.exception_handler.exception.service.ChatServiceException;
import com.example.domain.api.chat_service_api.mapper.ChatMapper;
import com.example.domain.api.chat_service_api.model.dto.ChatDTO;
import com.example.domain.api.chat_service_api.model.dto.ChatDetailsDTO;
import com.example.domain.api.chat_service_api.model.rest.chat.AssignChatRequestDTO;
import com.example.domain.api.chat_service_api.model.rest.chat.CloseChatRequestDTO;
import com.example.domain.api.chat_service_api.model.rest.chat.CreateChatRequestDTO;
import com.example.domain.api.chat_service_api.service.*;
import com.example.domain.api.chat_service_api.service.security.IChatSecurityService;
import com.example.domain.dto.AppUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatServiceImplTest {

    @Mock private ChatRepository chatRepository;
    @Mock private ChatMessageRepository chatMessageRepository;
    @Mock private IClientService clientService;
    @Mock private IUserService userService;
    @Mock private ChatMapper chatMapper;
    @Mock private IAssignmentService assignmentService;
    @Mock private WebSocketMessagingService messagingService;
    @Mock private IChatSecurityService chatSecurityService;
    @Mock private IAutoResponderService autoResponderService;
    @Mock private INotificationService notificationService;


    @InjectMocks private ChatServiceImpl chatService;

    private CreateChatRequestDTO createRequest;
    private Client client;
    private Company company;
    private Chat chat;
    private AppUserDetails operatorUser;
    private AppUserDetails managerUser;

    @BeforeEach
    void setUp() {
        createRequest = new CreateChatRequestDTO();
        createRequest.setClientId(1);
        createRequest.setChatChannel(ChatChannel.WhatsApp);
        createRequest.setInitialMessageContent("Test message");

        company = new Company();
        company.setId(1);

        client = new Client();
        client.setId(1);
        client.setCompany(company);


        chat = new Chat();
        chat.setId(1);
        chat.setClient(client);
        chat.setCompany(company);
        chat.setStatus(ChatStatus.PENDING_AUTO_RESPONDER);

        operatorUser = new AppUserDetails(1, 1, "operator@test.com", "password",
                Collections.singletonList(new SimpleGrantedAuthority(Role.OPERATOR.name())));

        managerUser = new AppUserDetails(2, 1, "manager@test.com", "password",
                Collections.singletonList(new SimpleGrantedAuthority(Role.MANAGER.name())));
    }

    // Основные тесты для создания чата
    @Test
    void createChat_ShouldCreateNewChatWhenValidRequest() {
        when(clientService.findById(1)).thenReturn(Optional.of(client));
        when(chatRepository.findFirstByClientIdAndChatChannelAndStatusInOrderByCreatedAtDesc(
                anyInt(), any(), any())).thenReturn(Optional.empty());
        when(chatRepository.save(any())).thenReturn(chat);
        when(chatMapper.toDetailsDto(any())).thenReturn(new ChatDetailsDTO());
        doNothing().when(autoResponderService).processNewPendingChat(anyInt());

        ChatDetailsDTO result = chatService.createChat(createRequest);

        assertNotNull(result);
        verify(chatRepository, times(2)).save(any());
    }

    @Test
    void createChat_ShouldThrowWhenOpenChatExists() {
        when(clientService.findById(1)).thenReturn(Optional.of(client));
        when(chatRepository.findFirstByClientIdAndChatChannelAndStatusInOrderByCreatedAtDesc(
                anyInt(), any(), any())).thenReturn(Optional.of(chat));

        assertThrows(ChatServiceException.class, () -> chatService.createChat(createRequest));
    }


    @Test
    void closeChat_ShouldThrowWhenAlreadyClosed() {
        // Arrange
        CloseChatRequestDTO request = new CloseChatRequestDTO();
        request.setChatId(1);
        chat.setStatus(ChatStatus.CLOSED);

        when(chatRepository.findById(1)).thenReturn(Optional.of(chat));

        // Act & Assert
        assertThrows(ChatServiceException.class, () -> chatService.closeChat(request));
        verify(chatRepository, never()).save(any());
    }
    // Тесты для получения чатов
    @Test
    void getOperatorChats_ShouldReturnChatsForOperator() {
        Set<ChatStatus> statuses = Set.of(ChatStatus.ASSIGNED, ChatStatus.IN_PROGRESS);
        chat.setUser(new User());
        chat.getUser().setId(1);

        when(chatRepository.findByUserIdAndStatusIn(1, statuses)).thenReturn(List.of(chat));
        when(chatMapper.toDto(any())).thenReturn(new ChatDTO());

        List<ChatDTO> result = chatService.getOperatorChats(1);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void getWaitingChats_ShouldReturnWaitingChatsForManager() {
        when(chatRepository.findByCompanyIdAndStatusOrderByLastMessageAtDesc(1, ChatStatus.PENDING_OPERATOR))
                .thenReturn(List.of(chat));
        when(chatMapper.toDto(any())).thenReturn(new ChatDTO());

        List<ChatDTO> result = chatService.getWaitingChats(1);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    // Тест для эскалации к оператору
    @Test
    void requestOperatorEscalation_ShouldChangeStatusWhenValid() {
        chat.setStatus(ChatStatus.PENDING_AUTO_RESPONDER);

        when(chatRepository.findById(1)).thenReturn(Optional.of(chat));
        doNothing().when(autoResponderService).stopForChat(1);
        when(chatRepository.save(any())).thenReturn(chat);
        when(chatMapper.toDetailsDto(any())).thenReturn(new ChatDetailsDTO());

        ChatDetailsDTO result = chatService.requestOperatorEscalation(1, 1);

        assertNotNull(result);
        assertEquals(ChatStatus.ASSIGNED, chat.getStatus());
    }

    @Test
    void getChatDetails_WhenChatExists_ShouldReturnDetails() {
        when(chatRepository.findById(1)).thenReturn(Optional.of(chat));
        when(chatMapper.toDetailsDto(any())).thenReturn(new ChatDetailsDTO());

        ChatDetailsDTO result = chatService.getChatDetails(1);

        assertNotNull(result);
    }

    @Test
    void getChatDetails_WhenChatNotExists_ShouldThrowException() {
        when(chatRepository.findById(1)).thenReturn(Optional.empty());

        assertThrows(ChatNotFoundException.class, () -> chatService.getChatDetails(1));
    }

    @Test
    void createChat_WhenClientExists_ShouldCreateChat() {
        when(clientService.findById(1)).thenReturn(Optional.of(client));
        when(chatRepository.findFirstByClientIdAndChatChannelAndStatusInOrderByCreatedAtDesc(
                anyInt(), any(), any())).thenReturn(Optional.empty());
        when(chatRepository.save(any())).thenReturn(chat);
        when(chatMessageRepository.save(any())).thenReturn(null);
        when(chatMapper.toDetailsDto(any())).thenReturn(new ChatDetailsDTO());

        doNothing().when(autoResponderService).processNewPendingChat(anyInt()); // ✅ заглушка

        ChatDetailsDTO result = chatService.createChat(createRequest);

        assertNotNull(result);
        verify(chatRepository, times(2)).save(any());
        verify(autoResponderService).processNewPendingChat(1); // ✅ проверка
    }


    @Test
    void createChat_WhenOpenChatExists_ShouldThrowException() {
        when(clientService.findById(1)).thenReturn(Optional.of(client));
        when(chatRepository.findFirstByClientIdAndChatChannelAndStatusInOrderByCreatedAtDesc(
                anyInt(), any(), any())).thenReturn(Optional.of(chat));

        assertThrows(ChatServiceException.class, () -> chatService.createChat(createRequest));
    }

    @Test
    void createChat_WhenClientNotExists_ShouldThrowException() {
        when(clientService.findById(1)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> chatService.createChat(createRequest));
    }


    @Test
    void assignOperatorToChat_WhenManagerWithAccess_ShouldSucceed() {
        // Arrange
        AssignChatRequestDTO request = new AssignChatRequestDTO();
        request.setChatId(1);
        request.setOperatorId(1);

        chat.setStatus(ChatStatus.PENDING_OPERATOR);
        User operator = new User();
        operator.setId(1);
        operator.setCompany(company);

        // Настраиваем security
        when(chatSecurityService.getCurrentAppUserPrincipal()).thenReturn(Optional.of(managerUser));
        when(chatSecurityService.canAssignOperatorToChat(1)).thenReturn(true);
        when(chatSecurityService.isAppUserOperatorOrManagerWithCompany(any())).thenReturn(true);

        // Настраиваем репозитории
        when(chatRepository.findById(1)).thenReturn(Optional.of(chat));
        when(userService.findById(1)).thenReturn(Optional.of(operator));
        when(chatRepository.save(any())).thenReturn(chat);
        when(assignmentService.assignOperator(any())).thenReturn(Optional.of(operator));
        when(chatMapper.toDetailsDto(any())).thenReturn(new ChatDetailsDTO());
        when(notificationService.createNotification(any(), any(), any(), any())).thenReturn(null);

        // Act
        ChatDetailsDTO result = chatService.assignOperatorToChat(request);

        // Assert
        assertNotNull(result);
        verify(chatSecurityService).canAssignOperatorToChat(1);
    }

    @Test
    void assignOperatorToChat_WhenUnauthorizedUser_ShouldThrowAccessDenied() {
        // Arrange
        AssignChatRequestDTO request = new AssignChatRequestDTO();
        request.setChatId(1);

        // Настраиваем security для неавторизованного пользователя
        when(chatSecurityService.getCurrentAppUserPrincipal()).thenReturn(Optional.empty());
        when(chatSecurityService.canAssignOperatorToChat(1)).thenReturn(false);
        when(chatSecurityService.isAppUserOperatorOrManagerWithCompany(any())).thenReturn(false);

        // Настраиваем репозиторий
        when(chatRepository.findById(1)).thenReturn(Optional.of(chat));

        // Act & Assert
        Exception exception = assertThrows(Exception.class, () -> chatService.assignOperatorToChat(request));
        assertTrue(exception instanceof AccessDeniedException ||
                "Access Denied".equals(exception.getMessage()));
    }

    @Test
    void closeChat_WhenNotAssignedOperator_ShouldThrowAccessDenied() {
        // 1. Подготовка тестовых данных
        CloseChatRequestDTO request = new CloseChatRequestDTO();
        request.setChatId(1);

        // Создаем другого оператора (не текущего)
        User otherOperator = new User();
        otherOperator.setId(2); // Отличается от operatorUser.getId() который равен 1
        otherOperator.setCompany(company);

        // Настраиваем чат с другим оператором
        chat.setUser(otherOperator);
        chat.setStatus(ChatStatus.ASSIGNED);

        // 2. Настройка моков
        // Важно: возвращаем false для canCloseChat
        when(chatSecurityService.canCloseChat(1)).thenReturn(false);

        // Эмулируем авторизованного оператора (но не назначенного на этот чат)
        when(chatSecurityService.getCurrentAppUserPrincipal()).thenReturn(Optional.of(operatorUser));

        // Чат существует
        when(chatRepository.findById(1)).thenReturn(Optional.of(chat));

        // 3. Запуск и проверка
        try {
            chatService.closeChat(request);
            fail("Expected AccessDeniedException but nothing was thrown");
        } catch (AccessDeniedException e) {
            // Успех - исключение поймано
            assertNotNull(e);
        } catch (Exception e) {
            fail("Expected AccessDeniedException but got " + e.getClass());
        }

        // 4. Дополнительные проверки
        verify(chatSecurityService).canCloseChat(1);
        verify(chatRepository, never()).save(any());
    }



}