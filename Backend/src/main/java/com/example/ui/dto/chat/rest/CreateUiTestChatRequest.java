package com.example.ui.dto.chat.rest;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CreateUiTestChatRequest {
    private Integer chatId;
    private String content;
}
