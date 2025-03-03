package com.example.domain.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MessageDto {

    @NotNull(message = "ChatMessage ID не может быть пустым")
    private Integer id;

    @NotNull(message = "Chat ID не может быть пустым")
    private ChatDto chatDto;

    @NotNull(message = "Content не может быть пустым")
    @Size(max = 500, message = "Сообщение не должно превышать 500 символов")
    private String content;

    private LocalDateTime sentAt;
}
