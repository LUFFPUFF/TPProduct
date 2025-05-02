package com.example.ui.dto.chat.rest;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UiSendUiMessageRequest {

    @NotNull(message = "Chat ID must not be null")
    private Integer chatId;

    @NotNull(message = "Content must not be null")
    private String content;
}
