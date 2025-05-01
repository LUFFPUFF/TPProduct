package com.example.domain.api.chat_service_api.controller;

import com.example.domain.api.chat_service_api.model.dto.MessageDto;
import com.example.domain.api.chat_service_api.model.rest.MarkMessagesAsReadRequestDTO;
import com.example.domain.api.chat_service_api.model.rest.RequestOperatorRequestDTO;
import com.example.domain.api.chat_service_api.model.rest.chat.AssignChatRequestDTO;
import com.example.domain.api.chat_service_api.model.rest.chat.CloseChatRequestDTO;
import com.example.domain.api.chat_service_api.model.rest.mesage.SendMessageRequestDTO;
import com.example.domain.api.chat_service_api.service.IChatMessageService;
import com.example.domain.api.chat_service_api.service.IChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ChatWebSocketController {

    private final IChatMessageService chatMessageService;
    private final IChatService chatService;

    /**
     * Обработка входящего сообщения от клиента или оператора.
     * Сообщение отправляется на /app/chat.sendMessage
     * DTO содержит senderId и senderType (ВРЕМЕННО).
     * @param messageRequest DTO с данными сообщения (chatId, content, senderId, senderType, etc.)
     * @return ChatMessageDTO сохраненного сообщения.
     */
    @MessageMapping("/chat.sendMessage")
    public MessageDto sendMessage(@Payload @Valid SendMessageRequestDTO messageRequest) {

        return chatMessageService.processAndSaveMessage(
                messageRequest,
                messageRequest.getSenderId(),
                messageRequest.getSenderType()
        );
    }

    /**
     * Обработка команды от пользователя (оператора): пометить сообщения как прочитанные.
     * Сообщение отправляется на /app/chat.markMessagesAsRead/{chatId}
     * DTO MarkMessagesAsReadRequestDTO содержит список ID сообщений и ID оператора (ВРЕМЕННО).
     * @param chatId ID чата (из пути).
     * @param request DTO, содержащее список ID сообщений и ID оператора.
     */
    @MessageMapping("/chat.markMessagesAsRead/{chatId}")
    public void markMessagesAsReadProper(@DestinationVariable Integer chatId,
                                         @Payload @Valid MarkMessagesAsReadRequestDTO request) {
        chatMessageService.markClientMessagesAsRead(chatId, request.getRequestingUserId(), request.getMessageIds());
    }

    /**
     * Обработка команды от клиента: запросить оператора.
     * Сообщение отправляется на /app/chat.requestOperator
     * DTO RequestOperatorRequestDTO содержит ID чата и ID клиента (ВРЕМЕННО).
     * @param request DTO с ID чата и ID клиента, запрашивающего оператора.
     */
    @MessageMapping("/chat.requestOperator")
    public void requestOperator(@Payload @Valid RequestOperatorRequestDTO request) {
        chatService.requestOperatorEscalation(request.getChatId(), request.getClientId());
    }

    /**
     * Обработка команды от оператора: взять чат в работу (claim).
     * Сообщение отправляется на /app/chat.claimChat
     * DTO AssignChatRequestDTO содержит ID чата и ID оператора, который берет чат (requestingUserId, ВРЕМЕННО).
     * @param assignRequest DTO с ID чата и ID оператора, который берет чат.
     */
    @MessageMapping("/chat.claimChat")
    public void claimChat(@Payload @Valid AssignChatRequestDTO assignRequest) {
        chatService.assignOperatorToChat(assignRequest);
    }

    /**
     * Обработка команды от оператора/админа: закрыть чат.
     * Сообщение отправляется на /app/chat.closeChat
     * DTO CloseChatRequestDTO содержит ID чата и ID пользователя, закрывающего чат (ВРЕМЕННО).
     * @param closeRequest DTO с ID чата и ID пользователя, закрывающего чат.
     */
    @MessageMapping("/chat.closeChat")
    public void closeChat(@Payload @Valid CloseChatRequestDTO closeRequest) {
        chatService.closeChat(closeRequest);
    }
}
