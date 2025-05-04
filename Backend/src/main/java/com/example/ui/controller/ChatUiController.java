package com.example.ui.controller;

import com.example.database.model.chats_messages_module.chat.Chat;
import com.example.database.model.chats_messages_module.chat.ChatMessageSenderType;
import com.example.database.model.chats_messages_module.chat.ChatStatus;
import com.example.database.model.company_subscription_module.company.Company;
import com.example.database.model.company_subscription_module.user_roles.user.User;
import com.example.database.model.crm_module.client.Client;
import com.example.domain.api.authentication_module.service.interfaces.UserDataService;
import com.example.domain.api.chat_service_api.exception_handler.ChatNotFoundException;
import com.example.domain.api.chat_service_api.exception_handler.ResourceNotFoundException;
import com.example.domain.api.chat_service_api.exception_handler.exception.service.ChatServiceException;
import com.example.domain.api.chat_service_api.model.dto.ChatDTO;
import com.example.domain.api.chat_service_api.model.dto.ChatDetailsDTO;
import com.example.domain.api.chat_service_api.model.dto.MessageDto;
import com.example.domain.api.chat_service_api.model.dto.notification.NotificationDTO;
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
    private final UserDataService userDataService;

    private Integer getCurrentOperatorId() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userDataService.getUserData(email).getUser().getId();
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

        Integer currentOperatorId = getCurrentOperatorId();

        List<ChatDTO> chats;
        if (statuses == null || statuses.isEmpty()) {
            chats = chatService.getOperatorChats(currentOperatorId);
        } else {
            chats = chatService.getOperatorChatsStatus(currentOperatorId, statuses);
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
        Integer currentOperatorId = getCurrentOperatorId();

        try {
            User operator = userService.findById(currentOperatorId)
                    .orElseThrow(() -> new ResourceNotFoundException("Operator with ID " + currentOperatorId + " not found."));

            Company company = operator.getCompany();
            if (company == null) {
                throw new ChatServiceException("Operator is not associated with a company.");
            }
            Integer companyId = company.getId();

            String testClientName = "Тестовый клиент (Оператор " + operator.getFullName() + ")";
            Client testClient = clientService.findByName(testClientName)
                    .orElseGet(() -> {
                        log.info("Creating new test client: {}", testClientName);
                        return clientService.createClient(testClientName, companyId, null);
                    });

            Optional<Chat> existingOpenTestChat = chatService.findOpenChatByClient(testClient.getId());
            if (existingOpenTestChat.isPresent()) {
                ChatDetailsDTO existingChatDetails = chatService.getChatDetails(existingOpenTestChat.get().getId());
                return ResponseEntity.ok(chatMapper.toUiDetailsDto(existingChatDetails));
            }

            CreateChatRequestDTO createRequest = new CreateChatRequestDTO();
            createRequest.setClientId(testClient.getId());
            createRequest.setCompanyId(companyId);
            createRequest.setChatChannel(null);
            createRequest.setInitialMessageContent("Добрый день! Я тестовый клиент.");

            ChatDetailsDTO initialChatDetails = chatService.createChat(createRequest);

            if (initialChatDetails.getStatus() != ChatStatus.ASSIGNED && initialChatDetails.getOperator() == null) {
                initialChatDetails.setStatus(ChatStatus.ASSIGNED);
            }

            ChatDetailsDTO currentChatDetails = chatService.getChatDetails(initialChatDetails.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(chatMapper.toUiDetailsDto(currentChatDetails));
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

        Integer currentOperatorId;

        Chat chatEntity = chatService.findChatEntityById(messageRequest.getChatId())
                .orElseThrow(() -> {
                    log.warn("Chat with ID {} not found for message sending.", messageRequest.getChatId());
                    return new ChatNotFoundException("Chat with ID " + messageRequest.getChatId() + " not found.");
                });

        User assignedOperator = chatEntity.getUser();

        if (assignedOperator == null) {
            throw new ChatServiceException("Cannot send message to chat without an assigned operator (TEMPORARY RESTRICTION). Chat status: " + chatEntity.getStatus());
        }

        currentOperatorId = assignedOperator.getId(); //TODO ВРЕМЕННО: Берем ID назначенного оператора

        if (chatEntity.getStatus() == ChatStatus.PENDING_AUTO_RESPONDER ||
                chatEntity.getStatus() == ChatStatus.CLOSED ||
                chatEntity.getStatus() == ChatStatus.ARCHIVED) {
            log.warn("Operator ID {} (assigned) attempted to send message to chat ID {} with invalid status {}.",
                    currentOperatorId, chatEntity.getId(), chatEntity.getStatus());
            throw new ChatServiceException("Cannot send message to chat with status: " + chatEntity.getStatus());
        }

        SendMessageRequestDTO serviceRequest = new SendMessageRequestDTO();
        serviceRequest.setChatId(chatEntity.getId());
        serviceRequest.setContent(messageRequest.getContent());
        serviceRequest.setSenderId(currentOperatorId);
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
        Integer currentOperatorId = getCurrentOperatorId();

        List<NotificationDTO> notifications;
        if (unreadOnly) {
            notifications = notificationService.getUnreadNotifications(currentOperatorId);
        } else {
            notifications = notificationService.getAllNotifications(currentOperatorId);
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
        Integer currentOperatorId = getCurrentOperatorId();
        chatMessageService.markClientMessagesAsRead(chatId, currentOperatorId, request.getMessageIds());
        return ResponseEntity.noContent().build();
    }

    /**
     * Закрыть чат текущим пользователем (оператором/админом) (по HTTP).
     * <p>ВАЖНО: WebSocket метод /app/chat.closeChat является предпочтительным.
     * <p>POST /api/ui/chats/{chatId}/close
     */
    @PostMapping("/{chatId}/close")
    public ResponseEntity<ChatUIDetailsDTO> closeChat(@PathVariable Integer chatId) {
        Integer currentUserId = getCurrentOperatorId();
        CloseChatRequestDTO serviceRequest = new CloseChatRequestDTO();
        serviceRequest.setChatId(chatId);
        serviceRequest.setClosingUserId(currentUserId);

        ChatDetailsDTO closedChatDetails = chatService.closeChat(serviceRequest);
        ChatUIDetailsDTO uiChatDetails = chatMapper.toUiDetailsDto(closedChatDetails);
        return ResponseEntity.ok(uiChatDetails);
    }
}
