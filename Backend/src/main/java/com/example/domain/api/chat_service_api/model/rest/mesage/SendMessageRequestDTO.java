package com.example.domain.api.chat_service_api.model.rest.mesage;

import com.example.database.model.chats_messages_module.chat.ChatMessageSenderType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SendMessageRequestDTO {

    @NotNull(message = "Chat ID must not be null")
    private Integer chatId;

    @NotBlank(message = "Content must not be blank")
    private String content;

    private String externalMessageId;

    private String replyToExternalMessageId;

    private Integer senderId;
    private ChatMessageSenderType senderType;
}
