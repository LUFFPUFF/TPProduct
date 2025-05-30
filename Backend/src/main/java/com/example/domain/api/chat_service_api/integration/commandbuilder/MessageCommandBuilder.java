package com.example.domain.api.chat_service_api.integration.commandbuilder;

import com.example.database.model.chats_messages_module.chat.Chat;
import com.example.database.model.chats_messages_module.chat.ChatChannel;
import com.example.domain.api.chat_service_api.exception_handler.ResourceNotFoundException;
import com.example.domain.api.chat_service_api.integration.listener.model.SendMessageCommand;
import com.example.domain.api.chat_service_api.integration.manager.widget.model.SenderInfoWidgetChat;

public interface MessageCommandBuilder {

    /**
     * Канал, для которого предназначена эта стратегия.
     */
    ChatChannel getSupportedChannel();

    /**
     * Создает SendMessageCommand для указанного чата и контента.
     *
     * @param chat Чат, для которого создается команда.
     * @param messageContent Содержимое сообщения.
     * @param senderInfo Информация об отправителе (может быть null для некоторых каналов).
     * @return Собранная SendMessageCommand.
     * @throws ResourceNotFoundException если необходимая конфигурация канала не найдена.
     * @throws IllegalArgumentException если предоставлены невалидные данные для канала.
     */
    SendMessageCommand buildCommand(Chat chat, String messageContent, SenderInfoWidgetChat senderInfo)
            throws ResourceNotFoundException, IllegalArgumentException;
}
