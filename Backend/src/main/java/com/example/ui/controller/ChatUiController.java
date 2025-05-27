package com.example.ui.controller;

import com.example.database.model.chats_messages_module.chat.ChatStatus;
import com.example.domain.api.authentication_module.service.interfaces.CurrentUserDataService;
import com.example.domain.api.chat_service_api.model.dto.ChatDTO;
import com.example.domain.api.chat_service_api.model.dto.ChatDetailsDTO;
import com.example.domain.api.chat_service_api.model.dto.MessageDto;
import com.example.domain.api.chat_service_api.model.rest.chat.AssignChatRequestDTO;
import com.example.domain.api.chat_service_api.model.rest.chat.CreateChatRequestDTO;
import com.example.domain.api.chat_service_api.service.chat.IChatService;
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
import org.springframework.web.bind.annotation.*;

import java.nio.file.AccessDeniedException;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/ui/chats")
@RequiredArgsConstructor
@Slf4j
public class ChatUiController {

    private final IChatService chatService;
    private final UiChatMapper chatMapper;
    private final UIMessageMapper messageMapper;
    private final UINotificationMapper notificationMapper;

    /**
     * Получает полные детали конкретного чата по его ID.
     * <p>GET /api/ui/chats/{chatId}/details
     */
    @GetMapping("/{chatId}/details")
    public ResponseEntity<ChatUIDetailsDTO> getChatDetails(@PathVariable Integer chatId) throws AccessDeniedException {
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
    public ResponseEntity<List<UIChatDto>> getMyChats(
            @RequestParam(value = "status", required = false) Set<ChatStatus> statuses
    ) {
        List<ChatDTO> chats = chatService.getChatsForCurrentUser(statuses);

        List<UIChatDto> uiChats = chats.stream()
                .map(chat -> {
                    UIChatDto uiChat = chatMapper.toUiDto(chat);
                    uiChat.setLastMessageContent(chat.getLastMessageSnippet());
                    return uiChat;
                })
                .toList();
        return ResponseEntity.ok(uiChats);
    }

    /**
     * Получает список чатов, назначенных конкретному оператору. Доступно только менеджерам.
     * <p>GET /api/ui/chats/operator/{operatorId}?status=...
     *
     * @param operatorId ID оператора, чьи чаты нужно получить.
     * @param statuses   Набор статусов для фильтрации.
     * @return Список UIChatDto.
     */
    @GetMapping("/operator/{operatorId}")
    public ResponseEntity<List<UIChatDto>> getOperatorChats(
            @PathVariable Integer operatorId,
            @RequestParam(value = "status", required = false) Set<ChatStatus> statuses
    ) {
        List<ChatDTO> chats = null;
        try {
            chats = chatService.getOperatorChats(operatorId, statuses);
        } catch (AccessDeniedException e) {
            throw new RuntimeException(e);
        }
        List<UIChatDto> uiChats = chats.stream()
                .map(chatMapper::toUiDto)
                .toList();
        return ResponseEntity.ok(uiChats);
    }

    /**
     * Получает список чатов для конкретного клиента. Доступно операторам и менеджерам в рамках своей компании.
     * <p>GET /api/ui/chats/client/{clientId}
     *
     * @param clientId ID клиента.
     * @return Список UIChatDto.
     */
    @GetMapping("/client/{clientId}")
    public ResponseEntity<List<UIChatDto>> getClientChats(@PathVariable Integer clientId) throws AccessDeniedException {
        List<ChatDTO> chats = chatService.getClientChats(clientId);
        List<UIChatDto> uiChats = chats.stream()
                .map(chatMapper::toUiDto)
                .toList();
        return ResponseEntity.ok(uiChats);
    }

    /**
     * Создает новый тестовый чат для текущего авторизованного оператора/менеджера с приветственным сообщением.
     * <p>POST /api/ui/chats/create-test-chat
     *
     * @return Детали созданного чата.
     */
    @PostMapping("/create-test-chat")
    public ResponseEntity<ChatUIDetailsDTO> createTestChat() {
        ChatDetailsDTO initialChatDetails = null;
        try {
            initialChatDetails = chatService.createTestChatForCurrentUser();
        } catch (AccessDeniedException e) {
            throw new RuntimeException(e);
        }
        ChatUIDetailsDTO uiChatDetails = chatMapper.toUiDetailsDto(initialChatDetails);
        return ResponseEntity.status(HttpStatus.CREATED).body(uiChatDetails);
    }

    /**
     * Отправляет сообщение в чат от лица текущего авторизованного оператора/менеджера через HTTP POST.
     * <p>ВАЖНО: WebSocket метод /app/chat.sendMessage является предпочтительным для чата.
     * <p>Этот эндпоинт может использоваться как fallback или для специфических случаев:
     * <p>POST /api/ui/chats/messages
     *
     * @param messageRequest DTO с данными сообщения (chatId, content).
     * @return UiMessageDto отправленного сообщения.
     */
    @PostMapping("/messages")
    public ResponseEntity<UiMessageDto> sendChatMessage(@RequestBody @Valid UiSendUiMessageRequest messageRequest) throws AccessDeniedException {
        MessageDto sentMessageDto = chatService.sendOperatorMessage(
                messageRequest.getChatId(),
                messageRequest.getContent()
        );
        UiMessageDto uiSentMessageDto = messageMapper.toUiDto(sentMessageDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(uiSentMessageDto);
    }

    /**
     * Создает новый чат с привязкой к текущему авторизованному оператору/менеджеру.
     * Используется из UI, например, при ручном создании чата из карточки клиента.
     * <p>POST /api/ui/chats
     *
     * @param createRequest DTO с данными для создания чата (client ID, channel, initial message).
     * @return Созданный ChatDetailsDTO или существующий открытый чат.
     */
    @PostMapping
    public ResponseEntity<ChatUIDetailsDTO> createChatWithOperatorFromUI(@RequestBody @Valid CreateChatRequestDTO createRequest) {
        ChatDetailsDTO chatDetails = null;
        try {
            chatDetails = chatService.createChatWithOperatorFromUI(createRequest);
        } catch (AccessDeniedException e) {
            throw new RuntimeException(e);
        }
        ChatUIDetailsDTO uiChatDetails = chatMapper.toUiDetailsDto(chatDetails);
        return ResponseEntity.status(HttpStatus.CREATED).body(uiChatDetails);
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
        throw new UnsupportedOperationException("Not implemented yet");
    }

    /**
     * Помечает сообщения в чате как прочитанные для текущего оператора (по HTTP).
     * <p>ВАЖНО: WebSocket метод /app/chat.markMessagesAsRead/{chatId} является предпочтительным.
     * <p>POST /api/ui/chats/{chatId}/messages/read
     */
    @PostMapping("/{chatId}/messages/read")
    public ResponseEntity<Void> markMessagesAsRead(@PathVariable Integer chatId,
                                                   @RequestBody @Valid MarkUIMessagesAsReadRequestUI request) throws AccessDeniedException {

        chatService.markClientMessagesAsReadByCurrentUser(chatId, request.getMessageIds());
        return ResponseEntity.noContent().build();
    }

    /**
     * Закрывает чат текущим авторизованным пользователем (оператором/менеджером) (по HTTP).
     * <p>ВАЖНО: WebSocket метод /app/chat.closeChat является предпочтительным.
     * <p>POST /api/ui/chats/{chatId}/close
     *
     * @param chatId ID чата для закрытия.
     * @return Детали закрытого чата.
     */
    @PostMapping("/{chatId}/close")
    public ResponseEntity<ChatUIDetailsDTO> closeChat(@PathVariable Integer chatId) throws AccessDeniedException {
        ChatDetailsDTO closedChatDetails = chatService.closeChatByCurrentUser(chatId);
        ChatUIDetailsDTO uiChatDetails = chatMapper.toUiDetailsDto(closedChatDetails);
        return ResponseEntity.ok(uiChatDetails);
    }

    /**
     * Получает список ожидающих чатов (статус PENDING_OPERATOR) для компании текущего авторизованного пользователя.
     * Доступно операторам и менеджерам.
     * <p>GET /api/ui/chats/waiting
     *
     * @return Список UIChatDto.
     */
    @GetMapping("/waiting")
    public ResponseEntity<List<UIChatDto>> getWaitingChats() {
        List<ChatDTO> chats = null;
        try {
            chats = chatService.getMyCompanyWaitingChats();
        } catch (AccessDeniedException e) {
            throw new RuntimeException(e);
        }
        List<UIChatDto> uiChats = chats.stream()
                .map(chatMapper::toUiDto)
                .toList();
        return ResponseEntity.ok(uiChats);
    }

    /**
     * Назначает оператора на чат. Доступно только менеджерам.
     * @param assignRequest DTO с ID чата и, опционально, ID назначаемого оператора.
     * @return Обновленный ChatDetailsDTO.
     */
    @PostMapping("/assign")
    public ResponseEntity<ChatUIDetailsDTO> assignChat(@RequestBody @Valid AssignChatRequestDTO assignRequest) {
        ChatDetailsDTO assignedChatDetails = null;
        try {
            assignedChatDetails = chatService.assignChat(assignRequest);
        } catch (AccessDeniedException e) {
            throw new RuntimeException(e);
        }
        ChatUIDetailsDTO uiChatDetails = chatMapper.toUiDetailsDto(assignedChatDetails);
        return ResponseEntity.ok(uiChatDetails);
    }

    /**
     * Запрашивает эскалацию чата до оператора.
     * <p>POST /api/ui/chats/{chatId}/escalate
     *
     * @param chatId ID чата.
     * @param clientId ID клиента чата (для верификации).
     * @return Обновленный ChatDetailsDTO.
     */
    @PostMapping("/{chatId}/escalate")
    public ResponseEntity<ChatUIDetailsDTO> requestOperatorEscalation(@PathVariable Integer chatId, @RequestParam Integer clientId) {
        ChatDetailsDTO updatedChatDetails = chatService.requestOperatorEscalation(chatId, clientId);
        ChatUIDetailsDTO uiChatDetails = chatMapper.toUiDetailsDto(updatedChatDetails);
        return ResponseEntity.ok(uiChatDetails);
    }

    /**
     * Привязывает указанного оператора к чату вручную. Доступно только менеджерам.
     * <p>POST /api/ui/chats/{chatId}/link-operator/{operatorId}
     *
     * @param chatId ID чата.
     * @param operatorId ID оператора для привязки.
     * @return Статус 204 No Content.
     */
    @PostMapping("/{chatId}/link-operator/{operatorId}")
    public ResponseEntity<Void> linkOperatorToChat(@PathVariable Integer chatId, @PathVariable Integer operatorId) throws AccessDeniedException {
        chatService.linkOperatorToChat(chatId, operatorId);
        return ResponseEntity.noContent().build();
    }
}
