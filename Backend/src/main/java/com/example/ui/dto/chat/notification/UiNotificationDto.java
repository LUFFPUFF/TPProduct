package com.example.ui.dto.chat.notification;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class UiNotificationDto {
    private Integer id;
    private Integer chatId;
    private String type;
    private String message;
    private LocalDateTime createdAt;
    private boolean read;
}
