package com.example.domain.api.chat_service_api.model.dto.notification;

import com.example.domain.api.chat_service_api.model.dto.ChatDTO;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NotificationDTO {
    private Integer id;
    private ChatDTO chat;
    private String type;
    private String message;
    private LocalDateTime createdAt;
    private boolean isRead;
}
