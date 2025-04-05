package com.example.domain.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChatDto {
    private Integer id;

    @NotNull(message = "Client ID не может быть пустым")
    private Integer clientId;

    @NotNull(message = "User ID не может быть пустым")
    private Integer userId;

    @NotNull(message = "Chat channel не может быть пустым")
    @Size(max = 50, message = "Канал чата не должен превышать 50 символов")
    private String chatChannel;

    @Size(max = 50, message = "Статус чата не должен превышать 50 символов")
    private String status;

    private LocalDateTime createdAt;
}
