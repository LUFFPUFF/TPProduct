package com.example.ui.dto.chat.message;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class UiMessageDto {

    private Integer id;
    private Integer chatId;
    private String senderType;
    private String senderName;
    private String content;
    private LocalDateTime sentAt;
    private String status;
}
