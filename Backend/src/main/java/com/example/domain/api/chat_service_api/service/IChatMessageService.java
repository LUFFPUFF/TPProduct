package com.example.domain.api.chat_service_api.service;

import com.example.database.model.chats_messages_module.chat.Chat;
import com.example.database.model.chats_messages_module.chat.ChatChannel;
import com.example.database.model.chats_messages_module.chat.ChatMessageSenderType;
import com.example.database.model.chats_messages_module.chat.ChatStatus;
import com.example.database.model.chats_messages_module.message.ChatMessage;
import com.example.database.model.chats_messages_module.message.MessageStatus;
import com.example.domain.api.chat_service_api.model.dto.MessageDto;
import com.example.domain.api.chat_service_api.model.rest.mesage.SendMessageRequestDTO;
import com.example.domain.security.model.UserContext;

import java.nio.file.AccessDeniedException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface IChatMessageService {

    /**
     * Обрабатывает и сохраняет новое сообщение в чате.
     * Включает создание сообщения, обновление связанных сущностей (например, lastMessageAt чата),
     * запуск метрик, публикацию событий и потенциальную отправку во внешние системы.
     *
     * @param messageRequest DTO с данными для отправки сообщения.
     * @param senderId ID отправителя (клиента или оператора).
     * @param senderType Тип отправителя.
     * @return DTO сохраненного сообщения.
     */
    MessageDto processAndSaveMessage(SendMessageRequestDTO messageRequest, Integer senderId, ChatMessageSenderType senderType);

    /**
     * Получает все сообщения для указанного чата, отсортированные по времени отправки.
     * Требует прав доступа к чату.
     *
     * @param chatId ID чата.
     * @return Список DTO сообщений.
     * @throws AccessDeniedException если у пользователя нет прав на просмотр этого чата.
     */
    List<MessageDto> getMessagesByChatId(Integer chatId) throws AccessDeniedException;

    /**
     * Обновляет статус сообщения.
     *
     * @param messageId ID сообщения.
     * @param newStatus Новый статус.
     * @param userContext Контекст пользователя, выполняющего действие.
     * @return DTO обновленного сообщения.
     * @throws AccessDeniedException если у пользователя нет прав на изменение статуса.
     */
    MessageDto updateMessageStatus(Integer messageId, MessageStatus newStatus, UserContext userContext) throws AccessDeniedException;

    /**
     * Помечает сообщения клиента как прочитанные указанным оператором.
     *
     * @param chatId ID чата.
     * @param operatorId ID оператора, который помечает сообщения.
     * @param messageIds Коллекция ID сообщений для пометки.
     * @param userContext Контекст пользователя, выполняющего действие.
     * @return Количество обновленных сообщений.
     * @throws AccessDeniedException если у пользователя нет прав на это действие.
     */
    int markClientMessagesAsRead(Integer chatId, Integer operatorId, Collection<Integer> messageIds, UserContext userContext) throws AccessDeniedException;


    /**
     * Находит первое сообщение в указанном чате.
     * Может требовать прав доступа к чату.
     *
     * @param chatId ID чата.
     * @return Optional с сущностью первого сообщения.
     * @throws AccessDeniedException если у пользователя нет прав на доступ к чату.
     */
    Optional<ChatMessage> findFirstMessageByChatId(Integer chatId) throws AccessDeniedException;

    /**
     * Обновляет статус сообщения оператора по его внешнему ID.
     * Обычно используется для обратной связи от внешних систем (например, статус доставки).
     *
     * @param chatId ID чата.
     * @param externalMessageId Внешний ID сообщения.
     * @param newStatus Новый статус.
     * @return Количество обновленных сообщений (0 или 1).
     */
    int updateOperatorMessageStatusByExternalId(Integer chatId, String externalMessageId, MessageStatus newStatus);

    Optional<Chat> findChatEntityById(Integer chatId);

    /**
     * @deprecated Этот метод не рекомендуется к использованию. Вместо него используйте processAndSaveMessage.
     * Сохраняет входящее сообщение от внешней системы.
     */
    @Deprecated
    ChatMessage saveIncomingMessageFromExternal(Integer chatId, ChatMessageSenderType senderType, Integer senderId, String content, String externalMessageId, String replyToExternalMessageId);
}
