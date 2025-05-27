package com.example.domain.api.chat_service_api.integration.dto;

import com.example.database.model.chats_messages_module.chat.ChatChannel;
import lombok.*;

@Data
@Builder
public class IncomingChannelMessage {

    @NonNull
    private String channelSpecificUserId;

    @NonNull
    private String messageContent;

    @NonNull
    private ChatChannel channel;

    private String externalChatId;

    private String replyToExternalMessageId;

}
