package com.example.ui.dto.chat.rest;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class MarkUIMessagesAsReadRequestUI {

    @NotEmpty(message = "Список ID сообщений не может быть пустым")
    private List<Integer> messageIds;
}
