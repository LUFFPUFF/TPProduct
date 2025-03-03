package com.example.domain.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChatDto {

    @NotNull(message = "Chat ID не может быть пустым")
    private Integer id;

//    @NotNull(message = "Client ID не может быть пустым")
    private ClientDto clientDto;
//
//    @NotNull(message = "User ID не может быть пустым")
    private UserDto userDto;

    @NotNull(message = "Chat channel не может быть пустым")
    private String chatChannel;

    @Size(max = 50, message = "Статус чата не должен превышать 50 символов")
    private String status;

    private LocalDateTime createdAt;
}
