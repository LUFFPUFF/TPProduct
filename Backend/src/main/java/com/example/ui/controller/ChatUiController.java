package com.example.ui.controller;

import com.example.database.model.chats_messages_module.chat.Chat;
import com.example.database.model.chats_messages_module.chat.ChatChannel;
import com.example.database.model.chats_messages_module.chat.ChatMessageSenderType;
import com.example.database.model.chats_messages_module.chat.ChatStatus;
import com.example.database.model.company_subscription_module.user_roles.user.User;
import com.example.database.model.crm_module.client.Client;
import com.example.database.repository.company_subscription_module.UserRepository;
import com.example.domain.api.chat_service_api.exception_handler.ChatNotFoundException;
import com.example.domain.api.chat_service_api.exception_handler.ResourceNotFoundException;
import com.example.domain.api.chat_service_api.exception_handler.exception.service.ChatServiceException;
import com.example.domain.api.chat_service_api.mapper.ChatMessageMapper;
import com.example.domain.api.chat_service_api.model.dto.ChatDTO;
import com.example.domain.api.chat_service_api.model.dto.ChatDetailsDTO;
import com.example.domain.api.chat_service_api.model.dto.MessageDto;
import com.example.domain.api.chat_service_api.model.dto.notification.NotificationDTO;
import com.example.domain.api.chat_service_api.model.dto.user.UserInfoDTO;
import com.example.domain.api.chat_service_api.model.rest.chat.AssignChatRequestDTO;
import com.example.domain.api.chat_service_api.model.rest.chat.CloseChatRequestDTO;
import com.example.domain.api.chat_service_api.model.rest.chat.CreateChatRequestDTO;
import com.example.domain.api.chat_service_api.model.rest.mesage.SendMessageRequestDTO;
import com.example.domain.api.chat_service_api.service.*;
import com.example.ui.mapper.chat.UIMessageMapper;
import com.example.ui.mapper.chat.UINotificationMapper;
import com.example.ui.mapper.chat.UiChatMapper;
import com.example.ui.dto.chat.ChatUIDetailsDTO;
import com.example.ui.dto.chat.UIChatDto;
import com.example.ui.dto.chat.message.UiMessageDto;
import com.example.ui.dto.chat.notification.UiNotificationDto;
import com.example.ui.dto.chat.rest.MarkUIMessagesAsReadRequestUI;
import com.example.ui.dto.chat.rest.UiSendUiMessageRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@RestController
@RequestMapping("/api/ui/chats")
@RequiredArgsConstructor
@Slf4j
public class ChatUiController {

    private final IChatService chatService;
    private final IChatMessageService chatMessageService;
    private final INotificationService notificationService;
    private final IUserService userService;
    private final IClientService clientService;

    private final UiChatMapper chatMapper;
    private final UIMessageMapper messageMapper;
    private final UINotificationMapper notificationMapper;
    private final ChatMessageMapper chatMessageMapper;

    private final UserRepository userRepository;

    private Optional<User> getCurrentAppUser(String email) {
        return userRepository.findByEmail(email);
    }

    /**
     * Получает полные детали конкретного чата по его ID.
     * <p>GET /api/ui/chats/{chatId}/details
     */
    @GetMapping("/{chatId}/details")
    public ResponseEntity<ChatUIDetailsDTO> getChatDetails(@PathVariable Integer chatId) {
        ChatDetailsDTO chatDetails = chatService.getChatDetails(chatId);
        ChatUIDetailsDTO uiChatDetails = chatMapper.toUiDetailsDto(chatDetails);
        return ResponseEntity.ok(uiChatDetails);
    }

    /**
     * Получает список диалогов (чатов) для текущего авторизованного оператора с возможностью фильтрации по статусу.
     * <p>GET /api/ui/chats/my?status=ASSIGNED&status=IN_PROGRESS
     *
     * <p>Дефолтное значение statuses = ASSIGNED, IN_PROGRESS
     *
     * @param statuses Список статусов для фильтрации (ASSIGNED, IN_PROGRESS, PENDING_OPERATOR, CLOSED, ARCHIVED).
     * @return Список UIChatDto.
     */
    @GetMapping("/my")
    public ResponseEntity<List<UIChatDto>> getMyOperatorsChats(
            @RequestParam(value = "status", required = false) Set<ChatStatus> statuses
    ) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        Optional<User> currentUserOpt = getCurrentAppUser(authentication.getName());

        User currentUser = currentUserOpt
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        List<ChatDTO> chats;
        if (statuses == null || statuses.isEmpty()) {
            chats = chatService.getOperatorChats(currentUser.getId());
        } else {
            chats = chatService.getOperatorChatsStatus(currentUser.getId(), statuses);
        }

        List<UIChatDto> uiChats = chats.stream()
                .map(chatMapper::toUiDto)
                .toList();

        return ResponseEntity.ok(uiChats);
    }

    /**
     * Создает новый тестовый чат для текущего оператора с приветственным сообщением от автоответчика.
     * <p>POST /api/ui/chats/create-test-chat
     *
     * @return Детали созданного чата.
     */
    @PostMapping("/create-test-chat")
    public ResponseEntity<ChatUIDetailsDTO> createTestChat() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        Optional<User> currentUserOpt = getCurrentAppUser(authentication.getName());

        User currentUser = currentUserOpt
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Integer currentUserId = currentUser.getId();
        Integer companyId = currentUser.getCompany().getId();

        if (companyId == null) {
            throw new ChatServiceException("Authenticated user is not associated with a company.");
        }

        try {
            User operator = userService.findById(currentUserId)
                    .orElseThrow(() -> new ResourceNotFoundException("Authenticated operator with ID " + currentUserId + " not found."));

            String testClientName = "Тестовый клиент (Оператор " + operator.getFullName() + ")";
            Client testClient = clientService.findByNameAndCompanyId(testClientName, companyId)
                    .orElseGet(() -> {
                        log.info("Creating new test client: {} for company ID {}", testClientName, companyId);
                        return clientService.createClient(testClientName, companyId, null);
                    });

            Optional<Chat> existingOpenTestChat = chatService.findOpenChatByClient(testClient.getId());
            if (existingOpenTestChat.isPresent()) {
                log.info("Open test chat ID {} already exists for client {}. Returning existing chat.", existingOpenTestChat.get().getId(), testClient.getId());
                ChatDetailsDTO existingChatDetails = chatService.getChatDetails(existingOpenTestChat.get().getId());
                return ResponseEntity.ok(chatMapper.toUiDetailsDto(existingChatDetails));
            }

            CreateChatRequestDTO createRequest = new CreateChatRequestDTO();
            createRequest.setClientId(testClient.getId());
            createRequest.setChatChannel(ChatChannel.Test);
            createRequest.setInitialMessageContent("Добрый день! Я тестовый клиент.");

            ChatDetailsDTO initialChatDetails = chatService.createChat(createRequest);
            UserInfoDTO userInfoDTO = chatMessageMapper.mapUserInfo(operator);
            initialChatDetails.setStatus(ChatStatus.ASSIGNED);
            initialChatDetails.setOperator(userInfoDTO);

            AssignChatRequestDTO assignRequest = new AssignChatRequestDTO();
            assignRequest.setChatId(initialChatDetails.getId());
            assignRequest.setOperatorId(currentUserId);

            ChatDetailsDTO assignedChatDetails = chatService.assignOperatorToChat(assignRequest);

            return ResponseEntity.status(HttpStatus.CREATED).body(chatMapper.toUiDetailsDto(assignedChatDetails));

        } catch (ResourceNotFoundException e) {
            log.error("Error creating test chat (resource not found): {}", e.getMessage());
            throw e;
        } catch (ChatServiceException e) {
            log.error("Error creating test chat (chat service exception): {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error creating test chat", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    /**
     * Отправляет сообщение в чат от лица текущего авторизованного оператора через HTTP POST.
     * <p>ВАЖНО: WebSocket метод /app/chat.sendMessage является предпочтительным для чата.
     * <p>Этот эндпоинт может использоваться как fallback или для специфических случаев:
     * <p>POST /api/ui/chats/messages
     *
     * @param messageRequest DTO с данными сообщения (chatId, content).
     * @return UiMessageDto отправленного сообщения.
     */
    @PostMapping("/messages")
    public ResponseEntity<UiMessageDto> sendChatMessage(@RequestBody @Valid UiSendUiMessageRequest messageRequest) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        Optional<User> currentUserOpt = getCurrentAppUser(authentication.getName());

        User currentUser = currentUserOpt
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Integer currentUserId = currentUser.getId();

        Chat chatEntity = chatService.findChatEntityById(messageRequest.getChatId())
                .orElseThrow(() -> {
                    log.warn("Chat with ID {} not found for message sending.", messageRequest.getChatId());
                    return new ChatNotFoundException("Chat with ID " + messageRequest.getChatId() + " not found.");
                });

        if (chatEntity.getStatus() == ChatStatus.PENDING_AUTO_RESPONDER ||
                chatEntity.getStatus() == ChatStatus.CLOSED ||
                chatEntity.getStatus() == ChatStatus.ARCHIVED) {
            log.warn("Operator ID {} attempted to send message to chat ID {} with invalid status {}.",
                    currentUserId, chatEntity.getId(), chatEntity.getStatus());
            throw new ChatServiceException("Cannot send message to chat with status: " + chatEntity.getStatus() + ". Allowed statuses: ASSIGNED, IN_PROGRESS.");
        }

        SendMessageRequestDTO serviceRequest = new SendMessageRequestDTO();
        serviceRequest.setChatId(chatEntity.getId());
        serviceRequest.setContent(messageRequest.getContent());
        serviceRequest.setSenderId(currentUserId);
        serviceRequest.setSenderType(ChatMessageSenderType.OPERATOR);

        MessageDto sentMessageDto = chatMessageService.processAndSaveMessage(
                serviceRequest,
                serviceRequest.getSenderId(),
                serviceRequest.getSenderType()
        );

        UiMessageDto uiSentMessageDto = messageMapper.toUiDto(sentMessageDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(uiSentMessageDto);
    }

    /**
     * Получает список уведомлений для текущего авторизованного оператора.
     * <p>GET /api/ui/notifications/my
     *
     * @param unreadOnly Флаг, указывающий, нужно ли получать только непрочитанные уведомления.
     * @return Список UINotificationDto.
     */
    @GetMapping("/notifications/my")
    public ResponseEntity<List<UiNotificationDto>> getMyNotifications(
            @RequestParam(value = "unreadOnly", required = false, defaultValue = "false") boolean unreadOnly
    ) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        Optional<User> currentUserOpt = getCurrentAppUser(authentication.getName());

        User currentUser = currentUserOpt
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Integer currentUserId = currentUser.getId();

        List<NotificationDTO> notifications;
        if (unreadOnly) {
            notifications = notificationService.getUnreadNotifications(currentUserId);
        } else {
            notifications = notificationService.getAllNotifications(currentUserId);
        }

        List<UiNotificationDto> uiNotifications = notifications.stream()
                .map(notificationMapper::toUiDto)
                .toList();

        return ResponseEntity.ok(uiNotifications);
    }

    /**
     * Помечает сообщения в чате как прочитанные для текущего оператора (по HTTP).
     * <p>ВАЖНО: WebSocket метод /app/chat.markMessagesAsRead/{chatId} является предпочтительным.
     * <p>POST /api/ui/chats/{chatId}/messages/read
     */
    @PostMapping("/{chatId}/messages/read")
    public ResponseEntity<Void> markMessagesAsRead(@PathVariable Integer chatId,
                                                   @RequestBody @Valid MarkUIMessagesAsReadRequestUI request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        Optional<User> currentUserOpt = getCurrentAppUser(authentication.getName());

        User currentUser = currentUserOpt
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Integer currentUserId = currentUser.getId();
        chatMessageService.markClientMessagesAsRead(chatId, currentUserId, request.getMessageIds());
        return ResponseEntity.noContent().build();
    }

    /**
     * Закрыть чат текущим пользователем (оператором/админом) (по HTTP).
     * <p>ВАЖНО: WebSocket метод /app/chat.closeChat является предпочтительным.
     * <p>POST /api/ui/chats/{chatId}/close
     */
    @PostMapping("/{chatId}/close")
    public ResponseEntity<ChatUIDetailsDTO> closeChat(@PathVariable Integer chatId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        Optional<User> currentUserOpt = getCurrentAppUser(authentication.getName());

        User currentUser = currentUserOpt
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Integer currentUserId = currentUser.getId();

        CloseChatRequestDTO serviceRequest = new CloseChatRequestDTO();
        serviceRequest.setChatId(chatId);
        serviceRequest.setClosingUserId(currentUserId);

        ChatDetailsDTO closedChatDetails = chatService.closeChat(serviceRequest);
        ChatUIDetailsDTO uiChatDetails = chatMapper.toUiDetailsDto(closedChatDetails);
        return ResponseEntity.ok(uiChatDetails);
    }

    @GetMapping("/waiting")
    public ResponseEntity<List<UIChatDto>> getWaitingChats() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        Optional<User> currentUserOpt = getCurrentAppUser(authentication.getName());

        User currentUser = currentUserOpt
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Integer companyId = currentUser.getCompany().getId();

        if (companyId == null) {
            log.warn("User {} attempted to get waiting chats but is not associated with a company.", currentUser.getId());
            throw new ChatServiceException("Authenticated user is not associated with a company.");
        }

        List<ChatDTO> chats = chatService.getWaitingChats(companyId);
        List<UIChatDto> uiChats = chats.stream()
                .map(chatMapper::toUiDto)
                .toList();

        return ResponseEntity.ok(uiChats);
    }

    @PostMapping("/assign")
    public ResponseEntity<ChatUIDetailsDTO> assignChat(@RequestBody @Valid AssignChatRequestDTO assignRequest) {
        ChatDetailsDTO assignedChatDetails = chatService.assignOperatorToChat(assignRequest);
        ChatUIDetailsDTO uiChatDetails = chatMapper.toUiDetailsDto(assignedChatDetails);

        return ResponseEntity.ok(uiChatDetails);
    }
}
