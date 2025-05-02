package com.example.ui.dto.chat;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class UIChatDto {

    private Integer id;
    private String clientName;
    private String source;
    private String lastMessageContent;
    private LocalDateTime lastMessageAt;
    private Integer unreadCount;
    private String status;
    private String assignedOperatorName;

}
