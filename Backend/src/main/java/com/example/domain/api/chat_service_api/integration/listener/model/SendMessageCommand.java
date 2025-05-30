package com.example.domain.api.chat_service_api.integration.listener.model;

import com.example.database.model.chats_messages_module.chat.ChatChannel;
import com.example.database.model.chats_messages_module.chat.ChatMessageSenderType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Команда для отправки сообщения во внешний канал.
 * Используется в исходящей очереди.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SendMessageCommand {

    private ChatChannel channel;

    private Integer chatId;

    private Integer companyId;

    private Long vkPeerId;

    private String content;

    private Long telegramChatId;

    private String toEmailAddress;

    private String fromEmailAddress;

    private String subject;

    private String whatsappRecipientPhoneNumber;

    private String dialogXChatSessionId;

    private ChatMessageSenderType senderType;

    private String senderId;

    private String senderDisplayName;
}
