package com.example.domain.api.chat_service_api.integration.sender;

import com.example.domain.api.chat_service_api.integration.exception.ChannelSenderException;
import com.example.domain.api.chat_service_api.integration.listener.model.SendMessageCommand;

public interface ChannelSender {

    /**
     * Отправляет сообщение через конкретный канал.
     * @param command Команда на отправку сообщения.
     * @throws ChannelSenderException если произошла ошибка при отправке.
     */
    void send(SendMessageCommand command) throws ChannelSenderException;
}
