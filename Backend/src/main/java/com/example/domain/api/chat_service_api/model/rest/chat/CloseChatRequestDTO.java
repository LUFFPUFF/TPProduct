package com.example.domain.api.chat_service_api.model.rest.chat;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CloseChatRequestDTO {
    @NotNull
    private Integer chatId;
    private Integer closingUserId;
}
