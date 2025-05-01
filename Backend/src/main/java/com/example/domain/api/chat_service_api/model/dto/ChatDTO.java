package com.example.domain.api.chat_service_api.model.dto;

import com.example.database.model.chats_messages_module.chat.ChatChannel;
import com.example.database.model.chats_messages_module.chat.ChatStatus;
import com.example.domain.api.chat_service_api.model.dto.client.ClientInfoDTO;
import com.example.domain.api.chat_service_api.model.dto.user.UserInfoDTO;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChatDTO {
    private Integer id;
    private ClientInfoDTO client;
    private UserInfoDTO operator;
    private ChatChannel chatChannel;
    private ChatStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime lastMessageAt;
    private String lastMessageSnippet;
    private int unreadMessagesCount;
}
