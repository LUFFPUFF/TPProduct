package com.example.domain.dto.chat_module;

import com.example.database.model.chats_messages_module.chat.ChatChannel;
import com.example.domain.dto.company_module.ClientDto;
import com.example.domain.dto.company_module.UserDto;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChatDto {

    private Integer id;

    @NotNull(message = "Client ID не может быть пустым")
    private ClientDto clientDto;

    @NotNull(message = "User ID не может быть пустым")
    private UserDto userDto;

    @NotNull(message = "Chat channel не может быть пустым")
    @Size(max = 50, message = "Канал чата не должен превышать 50 символов")
    private ChatChannel chatChannel;

    @Size(max = 50, message = "Статус чата не должен превышать 50 символов")
    private String status;

    private LocalDateTime createdAt;
}
