package com.example.domain.api.chat_service_api.integration.manager.widget.model;

import com.example.database.model.chats_messages_module.chat.ChatMessageSenderType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SenderInfoWidgetChat {

    private ChatMessageSenderType senderType;
    private String id;
    private String displayName;
}
