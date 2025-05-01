package com.example.domain.api.chat_service_api.model.rest.chat;

import com.example.database.model.chats_messages_module.chat.ChatChannel;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateChatRequestDTO {
    @NotNull
    private Integer clientId;

    @NotNull
    private Integer companyId;

    @NotNull(message = "Chat channel must not be null")
    private ChatChannel chatChannel;

    @Size(max = 1000)
    private String initialMessageContent;
}
