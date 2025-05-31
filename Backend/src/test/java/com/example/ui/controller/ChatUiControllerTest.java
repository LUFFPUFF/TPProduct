package com.example.ui.controller;

import com.example.database.model.chats_messages_module.chat.ChatChannel;
import com.example.database.model.chats_messages_module.chat.ChatMessageSenderType;
import com.example.database.model.chats_messages_module.chat.ChatStatus;
import com.example.database.model.chats_messages_module.message.MessageStatus;
import com.example.database.model.company_subscription_module.user_roles.user.User;
import com.example.domain.api.authentication_module.service.interfaces.CurrentUserDataService;
import com.example.domain.api.chat_service_api.exception_handler.ChatNotFoundException;
import com.example.domain.api.chat_service_api.model.dto.ChatDTO;
import com.example.domain.api.chat_service_api.model.dto.ChatDetailsDTO;
import com.example.domain.api.chat_service_api.model.dto.MessageDto;
import com.example.domain.api.chat_service_api.model.dto.client.ClientInfoDTO;
import com.example.domain.api.chat_service_api.model.dto.user.UserInfoDTO;
import com.example.domain.api.chat_service_api.model.rest.chat.AssignChatRequestDTO;
import com.example.domain.api.chat_service_api.model.rest.chat.CreateChatRequestDTO;
import com.example.domain.api.chat_service_api.service.IChatService;
import com.example.ui.dto.chat.ChatUIDetailsDTO;
import com.example.ui.dto.chat.UIChatDto;
import com.example.ui.dto.chat.message.UiMessageDto;
import com.example.ui.dto.chat.rest.MarkUIMessagesAsReadRequestUI;
import com.example.ui.dto.chat.rest.UiSendUiMessageRequest;
import com.example.ui.dto.user.UiUserDto;
import com.example.ui.mapper.chat.UIMessageMapper;
import com.example.ui.mapper.chat.UINotificationMapper;
import com.example.ui.mapper.chat.UiChatMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.nio.file.AccessDeniedException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatUiController Unit Tests")
class ChatUiControllerTest {

    @Mock
    private IChatService chatService;
    @Mock
    private UiChatMapper chatMapper;
    @Mock
    private UIMessageMapper messageMapper;
    @Mock
    private UINotificationMapper notificationMapper;

    @Mock(strictness = Mock.Strictness.LENIENT)
    private CurrentUserDataService userDataService;

    @InjectMocks
    private ChatUiController chatUiController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private final Integer MOCK_OPERATOR_ID = 1;
    private final Integer MOCK_CHAT_ID = 10;
    private final Integer MOCK_CLIENT_ID = 20;
    private final String MOCK_CLIENT_NAME = "Test Client Name";
    private final String MOCK_OPERATOR_NAME = "Test Operator Name";
    private final String MOCK_MESSAGE_CONTENT = "Hello from mock";


    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        mockMvc = MockMvcBuilders.standaloneSetup(chatUiController).build();

        User mockUser = new User();
        mockUser.setId(MOCK_OPERATOR_ID);
        when(userDataService.getUser()).thenReturn(mockUser);
    }

    // --- Хелперы для создания API DTO (которые приходят от сервиса) ---
    private ClientInfoDTO createMockApiClientInfoDTO() {
        ClientInfoDTO clientInfo = new ClientInfoDTO();
        clientInfo.setId(MOCK_CLIENT_ID);
        clientInfo.setName(MOCK_CLIENT_NAME);
        return clientInfo;
    }

    private UserInfoDTO createMockApiUserInfoDTO(Integer id, String name) {
        UserInfoDTO userInfo = new UserInfoDTO();
        userInfo.setId(id);
        userInfo.setFullName(name);
        return userInfo;
    }

    private ChatDetailsDTO createMockApiChatDetailsDTO() {
        ChatDetailsDTO dto = new ChatDetailsDTO();
        dto.setId(MOCK_CHAT_ID);
        dto.setStatus(ChatStatus.IN_PROGRESS);
        dto.setClient(createMockApiClientInfoDTO());
        dto.setOperator(createMockApiUserInfoDTO(MOCK_OPERATOR_ID, MOCK_OPERATOR_NAME));
        dto.setChatChannel(ChatChannel.Test);
        dto.setCreatedAt(LocalDateTime.now().minusHours(1));
        dto.setMessages(Collections.emptyList());
        dto.setLastMessageAt(LocalDateTime.now().minusMinutes(5));
        return dto;
    }

    private ChatDTO createMockApiChatDTO() {
        ChatDTO dto = new ChatDTO();
        dto.setId(MOCK_CHAT_ID);
        dto.setClient(createMockApiClientInfoDTO());
        dto.setOperator(createMockApiUserInfoDTO(MOCK_OPERATOR_ID, MOCK_OPERATOR_NAME));
        dto.setChatChannel(ChatChannel.Test);
        dto.setStatus(ChatStatus.IN_PROGRESS);
        dto.setCreatedAt(LocalDateTime.now().minusDays(1));
        dto.setLastMessageAt(LocalDateTime.now().minusMinutes(10));
        dto.setLastMessageSnippet(MOCK_MESSAGE_CONTENT);
        dto.setUnreadMessagesCount(1);
        return dto;
    }

    private MessageDto createMockApiMessageDto(int id, String content, ChatMessageSenderType senderType) {
        MessageDto dto = new MessageDto();
        dto.setId(id);
        dto.setContent(content);
        dto.setSenderType(senderType);
        if (senderType == ChatMessageSenderType.OPERATOR) {
            dto.setSenderOperator(createMockApiUserInfoDTO(MOCK_OPERATOR_ID, MOCK_OPERATOR_NAME));
        } else if (senderType == ChatMessageSenderType.CLIENT) {
            dto.setSenderClient(createMockApiClientInfoDTO());
        }
        dto.setSentAt(LocalDateTime.now());
        dto.setStatus(MessageStatus.DELIVERED);
        return dto;
    }


    // --- Тесты ---
    @Nested
    @DisplayName("GET /api/ui/chats/{chatId}/details")
    class GetChatDetails {
        @Test
        @DisplayName("should return ChatUIDetailsDTO on successful fetch")
        void getChatDetails_successful() throws Exception {
            ChatDetailsDTO apiDetailsDto = createMockApiChatDetailsDTO();
            ChatUIDetailsDTO mockUiDetailsDto = mock(ChatUIDetailsDTO.class);

            when(mockUiDetailsDto.getId()).thenReturn(apiDetailsDto.getId());
            when(mockUiDetailsDto.getStatus()).thenReturn(apiDetailsDto.getStatus().name());

            when(chatService.getChatDetails(MOCK_CHAT_ID)).thenReturn(apiDetailsDto);
            when(chatMapper.toUiDetailsDto(apiDetailsDto)).thenReturn(mockUiDetailsDto);

            mockMvc.perform(get("/api/ui/chats/{chatId}/details", MOCK_CHAT_ID)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(apiDetailsDto.getId()))
                    .andExpect(jsonPath("$.status").value(apiDetailsDto.getStatus().name()));

            verify(chatService).getChatDetails(MOCK_CHAT_ID);
            verify(chatMapper).toUiDetailsDto(apiDetailsDto);
        }

        @Test
        @DisplayName("should handle ChatNotFoundException from service")
        void getChatDetails_chatNotFound() throws Exception {
            ChatNotFoundException expectedRootCause = new ChatNotFoundException("Chat not found");
            when(chatService.getChatDetails(MOCK_CHAT_ID)).thenThrow(expectedRootCause);

            Exception caughtException = null;
            try {
                mockMvc.perform(get("/api/ui/chats/{chatId}/details", MOCK_CHAT_ID)
                                .accept(MediaType.APPLICATION_JSON))
                        .andReturn();
            } catch (ServletException e) {
                caughtException = e;
            }

            assertNotNull(caughtException);
            assertInstanceOf(ServletException.class, caughtException);
            assertInstanceOf(ChatNotFoundException.class, caughtException.getCause());
            assertEquals(expectedRootCause.getMessage(), caughtException.getCause().getMessage());
        }
    }

    @Nested
    @DisplayName("GET /api/ui/chats/my")
    class GetMyChats {
        @Test
        @DisplayName("should return list of UIChatDto for current user without status filter")
        void getMyChats_noStatus_successful() throws Exception {
            ChatDTO apiChatDto = createMockApiChatDTO();
            List<ChatDTO> apiChats = List.of(apiChatDto);

            UIChatDto mockUiChatDto = mock(UIChatDto.class);
            when(mockUiChatDto.getId()).thenReturn(apiChatDto.getId());

            when(chatService.getChatsForCurrentUser(null)).thenReturn(apiChats);
            when(chatMapper.toUiDto(apiChatDto)).thenReturn(mockUiChatDto);

            mockMvc.perform(get("/api/ui/chats/my")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(apiChatDto.getId()));

            verify(chatService).getChatsForCurrentUser(null);
            verify(chatMapper).toUiDto(apiChatDto);
            verify(mockUiChatDto).setLastMessageContent(apiChatDto.getLastMessageSnippet()); // Проверяем вызов сеттера
        }

        @Test
        @DisplayName("should return list of UIChatDto for current user with status filter")
        void getMyChats_withStatus_successful() throws Exception {
            Set<ChatStatus> statuses = Set.of(ChatStatus.IN_PROGRESS, ChatStatus.ASSIGNED);
            ChatDTO apiChatDto = createMockApiChatDTO();
            List<ChatDTO> apiChats = List.of(apiChatDto);

            UIChatDto mockUiChatDto = mock(UIChatDto.class);
            when(mockUiChatDto.getId()).thenReturn(apiChatDto.getId());

            when(chatService.getChatsForCurrentUser(statuses)).thenReturn(apiChats);
            when(chatMapper.toUiDto(apiChatDto)).thenReturn(mockUiChatDto);

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            statuses.forEach(s -> params.add("status", s.name()));

            mockMvc.perform(get("/api/ui/chats/my")
                            .params(params)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(apiChatDto.getId()));

            verify(chatService).getChatsForCurrentUser(statuses);
            verify(chatMapper).toUiDto(apiChatDto);
            verify(mockUiChatDto).setLastMessageContent(apiChatDto.getLastMessageSnippet());
        }
    }

    @Nested
    @DisplayName("GET /api/ui/chats/operator/{operatorId}")
    class GetOperatorChats {
        @Test
        @DisplayName("should return operator chats on successful fetch")
        void getOperatorChats_successful() throws Exception {
            List<ChatDTO> apiChats = List.of(createMockApiChatDTO());
            UIChatDto mockUiChatDto = mock(UIChatDto.class);
            when(mockUiChatDto.getId()).thenReturn(apiChats.get(0).getId());


            when(chatService.getOperatorChats(eq(MOCK_OPERATOR_ID), any())).thenReturn(apiChats);
            when(chatMapper.toUiDto(any(ChatDTO.class))).thenReturn(mockUiChatDto);

            mockMvc.perform(get("/api/ui/chats/operator/{operatorId}", MOCK_OPERATOR_ID)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(apiChats.get(0).getId()));

            verify(chatService).getOperatorChats(eq(MOCK_OPERATOR_ID), any());
            verify(chatMapper, times(apiChats.size())).toUiDto(any(ChatDTO.class));
        }

        @Test
        @DisplayName("should handle AccessDeniedException from service")
        void getOperatorChats_accessDenied() throws Exception {
            AccessDeniedException accessDenied = new AccessDeniedException("Access Denied");
            when(chatService.getOperatorChats(eq(MOCK_OPERATOR_ID), any())).thenThrow(accessDenied);

            Exception caughtException = null;
            try {
                mockMvc.perform(get("/api/ui/chats/operator/{operatorId}", MOCK_OPERATOR_ID)
                                .accept(MediaType.APPLICATION_JSON))
                        .andReturn();
            } catch (ServletException e) {
                caughtException = e;
            }

            assertNotNull(caughtException);
            assertInstanceOf(ServletException.class, caughtException);
            Throwable cause1 = caughtException.getCause();
            assertNotNull(cause1);
            assertInstanceOf(RuntimeException.class, cause1);
            Throwable cause2 = cause1.getCause();
            assertNotNull(cause2);
            assertInstanceOf(AccessDeniedException.class, cause2);
            assertEquals(accessDenied.getMessage(), cause2.getMessage());
        }
    }

    @Nested
    @DisplayName("GET /api/ui/chats/client/{clientId}")
    class GetClientChats {
        @Test
        @DisplayName("should return client chats on successful fetch")
        void getClientChats_successful() throws Exception {
            List<ChatDTO> apiChats = List.of(createMockApiChatDTO());
            UIChatDto mockUiChatDto = mock(UIChatDto.class);
            when(mockUiChatDto.getId()).thenReturn(apiChats.get(0).getId());

            when(chatService.getClientChats(MOCK_CLIENT_ID)).thenReturn(apiChats);
            when(chatMapper.toUiDto(any(ChatDTO.class))).thenReturn(mockUiChatDto);

            mockMvc.perform(get("/api/ui/chats/client/{clientId}", MOCK_CLIENT_ID)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(apiChats.get(0).getId()));

            verify(chatService).getClientChats(MOCK_CLIENT_ID);
            verify(chatMapper, times(apiChats.size())).toUiDto(any(ChatDTO.class));
        }
    }


    @Nested
    @DisplayName("POST /api/ui/chats/create-test-chat")
    class CreateTestChat {
        @Test
        @DisplayName("should return created ChatUIDetailsDTO on successful creation")
        void createTestChat_successful() throws Exception {
            ChatDetailsDTO apiDetailsDto = createMockApiChatDetailsDTO();
            ChatUIDetailsDTO mockUiDetailsDto = mock(ChatUIDetailsDTO.class);
            when(mockUiDetailsDto.getId()).thenReturn(apiDetailsDto.getId());


            when(chatService.createTestChatForCurrentUser()).thenReturn(apiDetailsDto);
            when(chatMapper.toUiDetailsDto(apiDetailsDto)).thenReturn(mockUiDetailsDto);

            mockMvc.perform(post("/api/ui/chats/create-test-chat")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(apiDetailsDto.getId()));

            verify(chatService).createTestChatForCurrentUser();
            verify(chatMapper).toUiDetailsDto(apiDetailsDto);
        }

        @Test
        @DisplayName("should handle AccessDeniedException from service")
        void createTestChat_accessDenied() throws Exception {
            AccessDeniedException accessDenied = new AccessDeniedException("Access Denied");
            when(chatService.createTestChatForCurrentUser()).thenThrow(accessDenied);

            Exception caughtException = null;
            try {
                mockMvc.perform(post("/api/ui/chats/create-test-chat")
                                .accept(MediaType.APPLICATION_JSON))
                        .andReturn();
            } catch (ServletException e) {
                caughtException = e;
            }

            assertNotNull(caughtException);
            assertInstanceOf(ServletException.class, caughtException);
            Throwable cause1 = caughtException.getCause();
            assertNotNull(cause1);
            assertInstanceOf(RuntimeException.class, cause1);
            Throwable cause2 = cause1.getCause();
            assertNotNull(cause2);
            assertInstanceOf(AccessDeniedException.class, cause2);
            assertEquals(accessDenied.getMessage(), cause2.getMessage());
        }
    }

    @Nested
    @DisplayName("POST /api/ui/chats/messages")
    class SendChatMessage {
        @Test
        @DisplayName("should send message and return UiMessageDto on successful send")
        void sendChatMessage_successful() throws Exception {
            UiSendUiMessageRequest request = UiSendUiMessageRequest.builder()
                    .chatId(MOCK_CHAT_ID)
                    .content("Hello from operator")
                    .build();

            MessageDto serviceResponseDto = createMockApiMessageDto(100, request.getContent(), ChatMessageSenderType.OPERATOR);
            UiMessageDto mockUiResponseDto = mock(UiMessageDto.class);
            when(mockUiResponseDto.getId()).thenReturn(serviceResponseDto.getId());
            when(mockUiResponseDto.getContent()).thenReturn(serviceResponseDto.getContent());


            when(chatService.sendOperatorMessage(request.getChatId(), request.getContent()))
                    .thenReturn(serviceResponseDto);
            when(messageMapper.toUiDto(serviceResponseDto)).thenReturn(mockUiResponseDto);

            mockMvc.perform(post("/api/ui/chats/messages")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(serviceResponseDto.getId()))
                    .andExpect(jsonPath("$.content").value(serviceResponseDto.getContent()));

            verify(chatService).sendOperatorMessage(request.getChatId(), request.getContent());
            verify(messageMapper).toUiDto(serviceResponseDto);
        }

        @Test
        @DisplayName("should handle ChatNotFoundException when sending message to non-existent chat")
        void sendChatMessage_chatNotFound() throws Exception {
            UiSendUiMessageRequest request = UiSendUiMessageRequest.builder()
                    .chatId(999) // Non-existent chat
                    .content("Test message")
                    .build();
            ChatNotFoundException expectedRootCause = new ChatNotFoundException("Chat not found for sending message");

            when(chatService.sendOperatorMessage(request.getChatId(), request.getContent()))
                    .thenThrow(expectedRootCause);

            Exception caughtException = null;
            try {
                mockMvc.perform(post("/api/ui/chats/messages")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                                .accept(MediaType.APPLICATION_JSON))
                        .andReturn();
            } catch (ServletException e) {
                caughtException = e;
            }

            assertNotNull(caughtException);
            assertInstanceOf(ServletException.class, caughtException);
            assertInstanceOf(ChatNotFoundException.class, caughtException.getCause());
            assertEquals(expectedRootCause.getMessage(), caughtException.getCause().getMessage());

            verify(chatService).sendOperatorMessage(request.getChatId(), request.getContent());
            verifyNoInteractions(messageMapper);
        }

        @Test
        @DisplayName("should return Bad Request for invalid UiSendUiMessageRequest (null content)")
        void sendChatMessage_invalidRequest_nullContent() throws Exception {
            UiSendUiMessageRequest request = UiSendUiMessageRequest.builder()
                    .chatId(MOCK_CHAT_ID)
                    .content(null)
                    .build();

            mockMvc.perform(post("/api/ui/chats/messages")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(chatService);
            verifyNoInteractions(messageMapper);
        }
    }

    @Nested
    @DisplayName("POST /api/ui/chats (createChatWithOperatorFromUI)")
    class CreateChatWithOperatorFromUI {
        @Test
        @DisplayName("should create chat and return ChatUIDetailsDTO on successful creation")
        void createChatWithOperator_successful() throws Exception {
            CreateChatRequestDTO createRequest = new CreateChatRequestDTO();
            createRequest.setClientId(MOCK_CLIENT_ID);
            createRequest.setCompanyId(1);
            createRequest.setChatChannel(ChatChannel.Email);
            createRequest.setInitialMessageContent("Initial message from UI");

            ChatDetailsDTO serviceResponseDto = createMockApiChatDetailsDTO();
            serviceResponseDto.setChatChannel(ChatChannel.Email);
            ChatUIDetailsDTO mockUiResponseDto = mock(ChatUIDetailsDTO.class);
            when(mockUiResponseDto.getId()).thenReturn(serviceResponseDto.getId());
            when(mockUiResponseDto.getChannel()).thenReturn(serviceResponseDto.getChatChannel().name());


            when(chatService.createChatWithOperatorFromUI(any(CreateChatRequestDTO.class)))
                    .thenReturn(serviceResponseDto);
            when(chatMapper.toUiDetailsDto(serviceResponseDto)).thenReturn(mockUiResponseDto);

            mockMvc.perform(post("/api/ui/chats")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createRequest))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(serviceResponseDto.getId()))
                    .andExpect(jsonPath("$.channel").value(ChatChannel.Email.name()));

            verify(chatService).createChatWithOperatorFromUI(any(CreateChatRequestDTO.class));
            verify(chatMapper).toUiDetailsDto(serviceResponseDto);
        }

        @Test
        @DisplayName("should handle AccessDeniedException from service")
        void createChatWithOperator_accessDenied() throws Exception {
            CreateChatRequestDTO createRequest = new CreateChatRequestDTO();
            createRequest.setClientId(MOCK_CLIENT_ID);
            createRequest.setCompanyId(1);
            createRequest.setChatChannel(ChatChannel.Telegram);

            AccessDeniedException accessDenied = new AccessDeniedException("Access Denied to create chat");
            when(chatService.createChatWithOperatorFromUI(any(CreateChatRequestDTO.class)))
                    .thenThrow(accessDenied);

            Exception caughtException = null;
            try {
                mockMvc.perform(post("/api/ui/chats")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(createRequest))
                                .accept(MediaType.APPLICATION_JSON))
                        .andReturn();
            } catch (ServletException e) {
                caughtException = e;
            }

            assertNotNull(caughtException);
            assertInstanceOf(ServletException.class, caughtException);
            Throwable cause1 = caughtException.getCause();
            assertNotNull(cause1);
            assertInstanceOf(RuntimeException.class, cause1);
            Throwable cause2 = cause1.getCause();
            assertNotNull(cause2);
            assertInstanceOf(AccessDeniedException.class, cause2);
            assertEquals(accessDenied.getMessage(), cause2.getMessage());

            verify(chatService).createChatWithOperatorFromUI(any(CreateChatRequestDTO.class));
            verifyNoInteractions(chatMapper);
        }

        @Test
        @DisplayName("should return Bad Request for invalid CreateChatRequestDTO (null channel)")
        void createChatWithOperator_invalidRequest_nullChannel() throws Exception {
            CreateChatRequestDTO createRequest = new CreateChatRequestDTO();
            createRequest.setClientId(MOCK_CLIENT_ID);
            createRequest.setCompanyId(1);
            createRequest.setChatChannel(null);

            mockMvc.perform(post("/api/ui/chats")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createRequest))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(chatService);
        }
    }

    @Nested
    @DisplayName("GET /api/ui/notifications/my")
    class GetMyNotifications {
        @Test
        @DisplayName("should throw ServletException wrapping UnsupportedOperationException")
        void getMyNotifications_throwsUnsupportedOperation() throws Exception {
            Exception caughtException = null;
            try {
                mockMvc.perform(get("/api/ui/chats/notifications/my")
                                .param("unreadOnly", "true")
                                .accept(MediaType.APPLICATION_JSON))
                        .andReturn();
            } catch (ServletException e) {
                caughtException = e;
            }

            assertNotNull(caughtException);
            assertInstanceOf(ServletException.class, caughtException);
            assertNotNull(caughtException.getCause());
            assertInstanceOf(UnsupportedOperationException.class, caughtException.getCause());
            assertEquals("Not implemented yet", caughtException.getCause().getMessage());

            verifyNoInteractions(chatService);
            verifyNoInteractions(notificationMapper);
        }
    }

    @Nested
    @DisplayName("POST /api/ui/chats/{chatId}/messages/read")
    class MarkMessagesAsRead {
        @Test
        @DisplayName("should return NoContent on successful marking messages as read")
        void markMessagesAsRead_successful() throws Exception {
            MarkUIMessagesAsReadRequestUI request = new MarkUIMessagesAsReadRequestUI();
            request.setMessageIds(List.of(1, 2, 3));

            doNothing().when(chatService).markClientMessagesAsReadByCurrentUser(MOCK_CHAT_ID, request.getMessageIds());

            mockMvc.perform(post("/api/ui/chats/{chatId}/messages/read", MOCK_CHAT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNoContent());

            verify(chatService).markClientMessagesAsReadByCurrentUser(MOCK_CHAT_ID, request.getMessageIds());
        }

        @Test
        @DisplayName("should handle ChatNotFoundException from service")
        void markMessagesAsRead_chatNotFound() throws Exception {
            MarkUIMessagesAsReadRequestUI request = new MarkUIMessagesAsReadRequestUI();
            request.setMessageIds(List.of(1));
            ChatNotFoundException expectedRootCause = new ChatNotFoundException("Chat not found to mark messages");

            doThrow(expectedRootCause).when(chatService)
                    .markClientMessagesAsReadByCurrentUser(MOCK_CHAT_ID, request.getMessageIds());

            Exception caughtException = null;
            try {
                mockMvc.perform(post("/api/ui/chats/{chatId}/messages/read", MOCK_CHAT_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andReturn();
            } catch (ServletException e) {
                caughtException = e;
            }

            assertNotNull(caughtException);
            assertInstanceOf(ServletException.class, caughtException);
            assertInstanceOf(ChatNotFoundException.class, caughtException.getCause());
            assertEquals(expectedRootCause.getMessage(), caughtException.getCause().getMessage());
        }

        @Test
        @DisplayName("should return Bad Request for invalid MarkUIMessagesAsReadRequestUI (empty list)")
        void markMessagesAsRead_invalidRequest_emptyList() throws Exception {
            MarkUIMessagesAsReadRequestUI request = new MarkUIMessagesAsReadRequestUI();
            request.setMessageIds(Collections.emptyList());

            mockMvc.perform(post("/api/ui/chats/{chatId}/messages/read", MOCK_CHAT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(chatService);
        }
    }

    @Nested
    @DisplayName("POST /api/ui/chats/{chatId}/close")
    class CloseChat {
        @Test
        @DisplayName("should return closed ChatUIDetailsDTO on successful close")
        void closeChat_successful() throws Exception {
            ChatDetailsDTO closedApiDetailsDto = createMockApiChatDetailsDTO();
            closedApiDetailsDto.setStatus(ChatStatus.CLOSED);
            ChatUIDetailsDTO mockUiDetailsDto = mock(ChatUIDetailsDTO.class);
            when(mockUiDetailsDto.getId()).thenReturn(closedApiDetailsDto.getId());
            when(mockUiDetailsDto.getStatus()).thenReturn(ChatStatus.CLOSED.name());


            when(chatService.closeChatByCurrentUser(MOCK_CHAT_ID)).thenReturn(closedApiDetailsDto);
            when(chatMapper.toUiDetailsDto(closedApiDetailsDto)).thenReturn(mockUiDetailsDto);

            mockMvc.perform(post("/api/ui/chats/{chatId}/close", MOCK_CHAT_ID)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(closedApiDetailsDto.getId()))
                    .andExpect(jsonPath("$.status").value(ChatStatus.CLOSED.name()));

            verify(chatService).closeChatByCurrentUser(MOCK_CHAT_ID);
            verify(chatMapper).toUiDetailsDto(closedApiDetailsDto);
        }

        @Test
        @DisplayName("should handle ChatNotFoundException from service")
        void closeChat_chatNotFound() throws Exception {
            ChatNotFoundException expectedRootCause = new ChatNotFoundException("Chat not found to close");
            when(chatService.closeChatByCurrentUser(MOCK_CHAT_ID)).thenThrow(expectedRootCause);

            Exception caughtException = null;
            try {
                mockMvc.perform(post("/api/ui/chats/{chatId}/close", MOCK_CHAT_ID)
                                .accept(MediaType.APPLICATION_JSON))
                        .andReturn();
            } catch (ServletException e) {
                caughtException = e;
            }

            assertNotNull(caughtException);
            assertInstanceOf(ServletException.class, caughtException);
            assertInstanceOf(ChatNotFoundException.class, caughtException.getCause());
            assertEquals(expectedRootCause.getMessage(), caughtException.getCause().getMessage());
        }

        @Test
        @DisplayName("should handle runtime AccessDeniedException from service if user cannot close chat")
        void closeChat_accessDenied() throws Exception {
            org.springframework.security.access.AccessDeniedException accessDenied =
                    new org.springframework.security.access.AccessDeniedException("User cannot close this chat");

            when(chatService.closeChatByCurrentUser(MOCK_CHAT_ID)).thenThrow(accessDenied);

            Exception caughtException = null;
            try {
                mockMvc.perform(post("/api/ui/chats/{chatId}/close", MOCK_CHAT_ID)
                                .accept(MediaType.APPLICATION_JSON))
                        .andReturn();
            } catch (ServletException e) {
                caughtException = e;
            }

            assertNotNull(caughtException, "Expected ServletException");
            assertInstanceOf(ServletException.class, caughtException);
            Throwable cause = caughtException.getCause();
            assertNotNull(cause, "ServletException should have a cause");
            assertInstanceOf(org.springframework.security.access.AccessDeniedException.class, cause);
            assertEquals(accessDenied.getMessage(), cause.getMessage());
        }
    }

    @Nested
    @DisplayName("GET /api/ui/chats/waiting")
    class GetWaitingChats {
        @Test
        @DisplayName("should return list of waiting UIChatDto on successful fetch")
        void getWaitingChats_successful() throws Exception {
            ChatDTO apiChatDto = createMockApiChatDTO();
            apiChatDto.setStatus(ChatStatus.PENDING_OPERATOR);
            List<ChatDTO> apiChats = List.of(apiChatDto);

            UIChatDto mockUiChatDto = mock(UIChatDto.class);
            when(mockUiChatDto.getId()).thenReturn(apiChatDto.getId());
            when(mockUiChatDto.getStatus()).thenReturn(ChatStatus.PENDING_OPERATOR.name());


            when(chatService.getMyCompanyWaitingChats()).thenReturn(apiChats);
            when(chatMapper.toUiDto(apiChatDto)).thenReturn(mockUiChatDto);

            mockMvc.perform(get("/api/ui/chats/waiting")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(apiChatDto.getId()))
                    .andExpect(jsonPath("$[0].status").value(ChatStatus.PENDING_OPERATOR.name()));

            verify(chatService).getMyCompanyWaitingChats();
            verify(chatMapper).toUiDto(apiChatDto);
            // Контроллер не вызывает mockUiChatDto.setLastMessageContent() здесь напрямую
        }

        @Test
        @DisplayName("should handle AccessDeniedException from service")
        void getWaitingChats_accessDenied() throws Exception {
            AccessDeniedException accessDenied = new AccessDeniedException("Access Denied to waiting chats");
            when(chatService.getMyCompanyWaitingChats()).thenThrow(accessDenied);

            Exception caughtException = null;
            try {
                mockMvc.perform(get("/api/ui/chats/waiting")
                                .accept(MediaType.APPLICATION_JSON))
                        .andReturn();
            } catch (ServletException e) {
                caughtException = e;
            }

            assertNotNull(caughtException);
            assertInstanceOf(ServletException.class, caughtException);
            Throwable cause1 = caughtException.getCause();
            assertNotNull(cause1);
            assertInstanceOf(RuntimeException.class, cause1);
            Throwable cause2 = cause1.getCause();
            assertNotNull(cause2);
            assertInstanceOf(AccessDeniedException.class, cause2);
            assertEquals(accessDenied.getMessage(), cause2.getMessage());
        }
    }

    @Nested
    @DisplayName("POST /api/ui/chats/assign")
    class AssignChat {
        @Test
        @DisplayName("should assign chat and return ChatUIDetailsDTO on successful assignment")
        void assignChat_successful() throws Exception {
            AssignChatRequestDTO assignRequest = new AssignChatRequestDTO();
            assignRequest.setChatId(MOCK_CHAT_ID);
            assignRequest.setOperatorId(MOCK_OPERATOR_ID + 1);

            ChatDetailsDTO apiDetailsDto = createMockApiChatDetailsDTO();
            UserInfoDTO assignedOperatorInfo = createMockApiUserInfoDTO(assignRequest.getOperatorId(), "Assigned Op");
            apiDetailsDto.setOperator(assignedOperatorInfo);

            ChatUIDetailsDTO mockUiDetailsDto = mock(ChatUIDetailsDTO.class);
            when(mockUiDetailsDto.getId()).thenReturn(apiDetailsDto.getId());
            UiUserDto mockUiOperator = mock(UiUserDto.class); // Мокаем UiUserDto
            when(mockUiOperator.getId()).thenReturn(assignRequest.getOperatorId());
            when(mockUiDetailsDto.getAssignedOperator()).thenReturn(mockUiOperator);


            when(chatService.assignChat(any(AssignChatRequestDTO.class))).thenReturn(apiDetailsDto);
            when(chatMapper.toUiDetailsDto(apiDetailsDto)).thenReturn(mockUiDetailsDto);

            mockMvc.perform(post("/api/ui/chats/assign")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(assignRequest))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(apiDetailsDto.getId()))
                    .andExpect(jsonPath("$.assignedOperator.id").value(assignRequest.getOperatorId()));

            verify(chatService).assignChat(any(AssignChatRequestDTO.class));
            verify(chatMapper).toUiDetailsDto(apiDetailsDto);
        }

        @Test
        @DisplayName("should handle AccessDeniedException from service")
        void assignChat_accessDenied() throws Exception {
            AssignChatRequestDTO assignRequest = new AssignChatRequestDTO();
            assignRequest.setChatId(MOCK_CHAT_ID);
            assignRequest.setOperatorId(MOCK_OPERATOR_ID + 1);

            AccessDeniedException accessDenied = new AccessDeniedException("Cannot assign chat");
            when(chatService.assignChat(any(AssignChatRequestDTO.class))).thenThrow(accessDenied);

            Exception caughtException = null;
            try {
                mockMvc.perform(post("/api/ui/chats/assign")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(assignRequest))
                                .accept(MediaType.APPLICATION_JSON))
                        .andReturn();
            } catch (ServletException e) {
                caughtException = e;
            }

            assertNotNull(caughtException);
            assertInstanceOf(ServletException.class, caughtException);
            Throwable cause1 = caughtException.getCause();
            assertNotNull(cause1);
            assertInstanceOf(RuntimeException.class, cause1);
            Throwable cause2 = cause1.getCause();
            assertNotNull(cause2);
            assertInstanceOf(AccessDeniedException.class, cause2);
            assertEquals(accessDenied.getMessage(), cause2.getMessage());
        }

        @Test
        @DisplayName("should return Bad Request for invalid AssignChatRequestDTO (null chatId)")
        void assignChat_invalidRequest_nullChatId() throws Exception {
            AssignChatRequestDTO assignRequest = new AssignChatRequestDTO();
            assignRequest.setChatId(null);
            assignRequest.setOperatorId(MOCK_OPERATOR_ID);

            mockMvc.perform(post("/api/ui/chats/assign")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(assignRequest))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(chatService);
            verifyNoInteractions(chatMapper);
        }
    }

    @Nested
    @DisplayName("POST /api/ui/chats/{chatId}/escalate")
    class RequestOperatorEscalation {
        @Test
        @DisplayName("should escalate chat and return ChatUIDetailsDTO on successful escalation")
        void requestOperatorEscalation_successful() throws Exception {
            ChatDetailsDTO apiDetailsDto = createMockApiChatDetailsDTO();
            apiDetailsDto.setStatus(ChatStatus.PENDING_OPERATOR);
            ChatUIDetailsDTO mockUiDetailsDto = mock(ChatUIDetailsDTO.class);
            when(mockUiDetailsDto.getId()).thenReturn(apiDetailsDto.getId());
            when(mockUiDetailsDto.getStatus()).thenReturn(ChatStatus.PENDING_OPERATOR.name());


            when(chatService.requestOperatorEscalation(MOCK_CHAT_ID, MOCK_CLIENT_ID)).thenReturn(apiDetailsDto);
            when(chatMapper.toUiDetailsDto(apiDetailsDto)).thenReturn(mockUiDetailsDto);

            mockMvc.perform(post("/api/ui/chats/{chatId}/escalate", MOCK_CHAT_ID)
                            .param("clientId", String.valueOf(MOCK_CLIENT_ID))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(apiDetailsDto.getId()))
                    .andExpect(jsonPath("$.status").value(ChatStatus.PENDING_OPERATOR.name()));

            verify(chatService).requestOperatorEscalation(MOCK_CHAT_ID, MOCK_CLIENT_ID);
            verify(chatMapper).toUiDetailsDto(apiDetailsDto);
        }

        @Test
        @DisplayName("should handle ChatNotFoundException from service")
        void requestOperatorEscalation_chatNotFound() throws Exception {
            ChatNotFoundException expectedRootCause = new ChatNotFoundException("Chat not found for escalation");
            when(chatService.requestOperatorEscalation(MOCK_CHAT_ID, MOCK_CLIENT_ID))
                    .thenThrow(expectedRootCause);

            Exception caughtException = null;
            try {
                mockMvc.perform(post("/api/ui/chats/{chatId}/escalate", MOCK_CHAT_ID)
                                .param("clientId", String.valueOf(MOCK_CLIENT_ID))
                                .accept(MediaType.APPLICATION_JSON))
                        .andReturn();
            } catch (ServletException e) {
                caughtException = e;
            }

            assertNotNull(caughtException);
            assertInstanceOf(ServletException.class, caughtException);
            assertInstanceOf(ChatNotFoundException.class, caughtException.getCause());
            assertEquals(expectedRootCause.getMessage(), caughtException.getCause().getMessage());
        }
    }

    @Nested
    @DisplayName("POST /api/ui/chats/{chatId}/link-operator/{operatorId}")
    class LinkOperatorToChat {
        @Test
        @DisplayName("should return NoContent on successful linking")
        void linkOperatorToChat_successful() throws Exception {
            Integer targetOperatorId = MOCK_OPERATOR_ID + 2;
            doNothing().when(chatService).linkOperatorToChat(MOCK_CHAT_ID, targetOperatorId);

            mockMvc.perform(post("/api/ui/chats/{chatId}/link-operator/{operatorId}", MOCK_CHAT_ID, targetOperatorId))
                    .andExpect(status().isNoContent());

            verify(chatService).linkOperatorToChat(MOCK_CHAT_ID, targetOperatorId);
        }

        @Test
        @DisplayName("should handle ChatNotFoundException from service")
        void linkOperatorToChat_chatNotFound() throws Exception {
            Integer targetOperatorId = MOCK_OPERATOR_ID + 2;
            ChatNotFoundException expectedRootCause = new ChatNotFoundException("Chat not found for linking");
            doThrow(expectedRootCause).when(chatService).linkOperatorToChat(MOCK_CHAT_ID, targetOperatorId);

            Exception caughtException = null;
            try {
                mockMvc.perform(post("/api/ui/chats/{chatId}/link-operator/{operatorId}", MOCK_CHAT_ID, targetOperatorId))
                        .andReturn();
            } catch (ServletException e) {
                caughtException = e;
            }

            assertNotNull(caughtException);
            assertInstanceOf(ServletException.class, caughtException);
            assertInstanceOf(ChatNotFoundException.class, caughtException.getCause());
            assertEquals(expectedRootCause.getMessage(), caughtException.getCause().getMessage());
        }

        @Test
        @DisplayName("should handle runtime AccessDeniedException (if service throws it)")
        void linkOperatorToChat_accessDenied() throws Exception {
            Integer targetOperatorId = MOCK_OPERATOR_ID + 2;
            org.springframework.security.access.AccessDeniedException accessDenied =
                    new org.springframework.security.access.AccessDeniedException("Cannot link operator to this chat");
            doThrow(accessDenied).when(chatService).linkOperatorToChat(MOCK_CHAT_ID, targetOperatorId);

            Exception caughtException = null;
            try {
                mockMvc.perform(post("/api/ui/chats/{chatId}/link-operator/{operatorId}", MOCK_CHAT_ID, targetOperatorId))
                        .andReturn();
            } catch (ServletException e) {
                caughtException = e;
            }

            assertNotNull(caughtException);
            assertInstanceOf(ServletException.class, caughtException);
            Throwable cause = caughtException.getCause();
            assertNotNull(cause);
            assertInstanceOf(org.springframework.security.access.AccessDeniedException.class, cause);
            assertEquals(accessDenied.getMessage(), cause.getMessage());
        }
    }
}