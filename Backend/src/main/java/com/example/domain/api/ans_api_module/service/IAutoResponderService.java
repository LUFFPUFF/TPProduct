package com.example.domain.api.ans_api_module.service;

import com.example.database.model.chats_messages_module.chat.Chat;
import com.example.domain.api.ans_api_module.exception.AutoResponderException;
import com.example.domain.api.chat_service_api.model.dto.MessageDto;

public interface IAutoResponderService {

    /**
     * Инициирует работу автоответчика для нового чата в статусе PENDING_AUTO_RESPONDER.
     * Может отправить приветственное сообщение или сразу начать анализ первого сообщения.
     * @param chatId ID чата.
     * @throws AutoResponderException В случае ошибки в работе автоответчика.
     */
    void processNewPendingChat(Integer chatId) throws AutoResponderException;

    /**
     * Обрабатывает входящее сообщение в чате, который находится под управлением автоответчика.
     * Анализирует сообщение, ищет ответы и отправляет ответ (или переводит чат на оператора).
     * @param messageDTO DTO входящего сообщения (от клиента).
     * @throws AutoResponderException В случае ошибки в работе автоответчика.
     */
    void processIncomingMessage(MessageDto messageDTO, Chat chat) throws AutoResponderException;

    /**
     * Останавливает работу автоответчика для указанного чата.
     * Вызывается, например, ChatService при эскалации чата на оператора.
     * Включает любую необходимую очистку состояния или отмену запланированных задач.
     * @param chatId ID чата.
     */
    void stopForChat(Integer chatId);

}
