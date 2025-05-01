package com.example.domain.api.chat_service_api.model.dto;

import com.example.database.model.chats_messages_module.message.MessageStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MessageStatusUpdateDTO {
    @NotNull
    private Integer messageId;
    @NotNull
    private Integer chatId;
    @NotNull
    private MessageStatus newStatus;
    private LocalDateTime timestamp;
}
