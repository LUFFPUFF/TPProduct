package com.example.domain.api.chat_service_api.integration.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DialogXChatOutgoingMessage {

    private String type;
    private String text;
    private String senderName;
    private Long serverTimestamp;
}
