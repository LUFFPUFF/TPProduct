package com.example.domain.api.chat_service_api.service;

import com.example.database.model.chats_messages_module.chat.Chat;
import com.example.database.model.chats_messages_module.chat.ChatChannel;
import com.example.database.model.chats_messages_module.chat.ChatMessageSenderType;
import com.example.database.model.chats_messages_module.chat.ChatStatus;
import com.example.database.model.chats_messages_module.message.ChatMessage;
import com.example.database.model.chats_messages_module.message.MessageStatus;
import com.example.domain.api.chat_service_api.model.dto.MessageDto;
import com.example.domain.api.chat_service_api.model.rest.mesage.SendMessageRequestDTO;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface IChatMessageService {

    /**
     * Обрабатывает входящее сообщение (сохраняет и рассылает по WebSocket).
     * @param messageRequest DTO с данными сообщения.
     * @param senderId ID отправителя (клиента или пользователя).
     * @param senderType Тип отправителя.
     * @return Сохраненный ChatMessageDTO.
     */
    MessageDto processAndSaveMessage(SendMessageRequestDTO messageRequest, Integer senderId, ChatMessageSenderType senderType);

    /**
     * Получает список сообщений для конкретного чата, отсортированный по времени отправки.
     * @param chatId ID чата.
     * @return Список ChatMessageDTO.
     */
    List<MessageDto> getMessagesByChatId(Integer chatId);

    /**
     * Обновляет статус сообщения.
     * @param messageId ID сообщения.
     * @param newStatus Новый статус.
     * @return Обновленный ChatMessageDTO или null, если не найдено/нет прав.
     */
    MessageDto updateMessageStatus(Integer messageId, MessageStatus newStatus);

    /**
     * Помечает сообщения клиента в чате как прочитанные оператором.
     * @param chatId ID чата.
     * @param operatorId ID оператора, который прочитал.
     * @param messageIds Список ID сообщений клиента для пометки.
     * @return Количество обновленных сообщений.
     */
    int markClientMessagesAsRead(Integer chatId, Integer operatorId, Collection<Integer> messageIds);

    Optional<Chat> findOpenChatByClientAndChannel(Integer clientId, ChatChannel channel);

    /**
     * Находит первое (самое старое по sentAt) сообщение в указанном чате.
     * @param chatId ID чата.
     * @return Optional ChatMessage Entity.
     */
    Optional<ChatMessage> findFirstMessageByChatId(Integer chatId);

    Optional<Chat> findChatEntityById(Integer chatId);

    /**
     * Обновляет статус операторского сообщения по внешнему ID.
     * @param chatId ID чата.
     * @param externalMessageId Внешний ID сообщения оператора.
     * @param newStatus Новый статус.
     * @return Количество обновленных сообщений.
     */
    int updateOperatorMessageStatusByExternalId(Integer chatId, String externalMessageId, MessageStatus newStatus);

    ChatMessage saveIncomingMessageFromExternal(Integer chatId,
                                                ChatMessageSenderType senderType,
                                                Integer senderId, String content,
                                                String externalMessageId,
                                                String replyToExternalMessageId);
}
