package com.example.domain.api.chat_service_api.integration.listener;

import com.example.database.model.chats_messages_module.chat.ChatChannel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Команда для отправки сообщения во внешний канал.
 * Используется в исходящей очереди.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SendMessageCommand {
    private ChatChannel channel;
    private Integer chatId;

    private String content;

    private Long telegramChatId;
    // TODO: Возможно, добавить botToken или botUsername, если адаптер не синглтон или много ботов

    private String toEmailAddress;
    private String fromEmailAddress;
    private String subject;

}
