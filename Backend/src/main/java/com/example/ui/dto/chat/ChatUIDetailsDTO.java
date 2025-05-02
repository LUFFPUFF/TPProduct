package com.example.ui.dto.chat;

import com.example.ui.dto.chat.message.UiMessageDto;
import com.example.ui.dto.client.UiClientDto;
import com.example.ui.dto.user.UiUserDto;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ChatUIDetailsDTO {
    private Integer id;
    private String status;
    private UiClientDto client;
    private UiUserDto assignedOperator;
    private String channel;
    private LocalDateTime createdAt;
    private LocalDateTime assignedAt;
    private LocalDateTime closedAt;
    private List<UiMessageDto> messages;
}
