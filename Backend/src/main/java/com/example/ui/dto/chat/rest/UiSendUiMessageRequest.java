package com.example.ui.dto.chat.rest;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UiSendUiMessageRequest {

    @NotNull(message = "Chat ID must not be null")
    @JsonProperty("chatId")
    private Integer chatId;

    @NotNull(message = "Content must not be null")
    @JsonProperty("content")
    private String content;
}
