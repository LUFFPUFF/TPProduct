package com.example.domain.api.chat_service_api.integration.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class DialogXChatDto {

    private Integer id;
    private String widgetId;
    private Integer companyId;
    private boolean enabled;
    private String welcomeMessage;
    private String widgetScriptCode;
    private String themeColor;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
