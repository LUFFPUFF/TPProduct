package com.example.domain.api.chat_service_api.integration.service;

import com.example.domain.api.chat_service_api.exception_handler.exception.ExternalMessagingException;

public interface IExternalMessagingService {

    /**
     * Отправляет сообщение в тот внешний канал, из которого пришел чат.
     * @param chatId ID чата.
     * @param messageContent Содержимое сообщения для отправки.
     * @throws ExternalMessagingException В случае ошибки при определении канала, получении данных
     *                                     или отправке во внешний сервис.
     */
    void sendMessageToExternal(Integer chatId, String messageContent) throws ExternalMessagingException;

    // TODO: Возможно, добавить метод для отправки с внешним ID сообщения, если это нужно для статусов
    // void sendMessageToExternal(Integer chatId, String messageContent, String replyToExternalMessageId);

}
