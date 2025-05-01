package com.example.domain.api.chat_service_api.model.dto;

import com.example.database.model.chats_messages_module.chat.ChatMessageSenderType;
import com.example.database.model.chats_messages_module.message.MessageStatus;
import com.example.domain.api.chat_service_api.model.dto.client.ClientInfoDTO;
import com.example.domain.api.chat_service_api.model.dto.user.UserInfoDTO;
import com.example.domain.dto.ChatDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MessageDto {
    @NotNull(message = "ID сообщения обязательно")
    private Integer id;

    @NotNull(message = "Чат обязателен")
    @Valid
    private ChatDto chatDto;

    @NotBlank(message = "Содержание сообщения не может быть пустым")
    @Size(max = 500, message = "Сообщение не должно превышать 500 символов")
    private String content;

    @NotNull(message = "Тип отправителя обязателен")
    private ChatMessageSenderType senderType;

    private MessageStatus status;

    private UserInfoDTO senderOperator;
    private ClientInfoDTO senderClient;

    private String replyToExternalMessageId;

    @PastOrPresent(message = "Дата отправки должна быть в прошлом или настоящем")
    private LocalDateTime sentAt;
}
