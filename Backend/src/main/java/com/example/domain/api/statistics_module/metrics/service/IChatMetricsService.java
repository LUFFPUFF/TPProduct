package com.example.domain.api.statistics_module.metrics.service;

import com.example.database.model.chats_messages_module.chat.ChatChannel;
import com.example.database.model.chats_messages_module.chat.ChatMessageSenderType;
import com.example.database.model.chats_messages_module.chat.ChatStatus;

import java.time.Duration;

public interface IChatMetricsService {

    void incrementChatsCreated(String companyId, ChatChannel channel, boolean fromOperatorUI);
    void incrementChatsAssigned(String companyId, ChatChannel channel, boolean autoAssigned);
    void incrementChatsClosed(String companyId, ChatChannel channel, ChatStatus finalStatus);
    void incrementChatsEscalated(String companyId, ChatChannel channel);
    void incrementChatsAutoResponderHandled(String companyId, ChatChannel channel);
    void incrementChatOperatorLinked(String companyId, ChatChannel channel);

    void recordChatDuration(String companyId, ChatChannel channel, ChatStatus finalStatus, Duration duration);
    void recordChatAssignmentTime(String companyId, ChatChannel channel, Duration duration);
    void recordChatFirstOperatorResponseTime(String companyId, ChatChannel channel, Duration duration);

    void incrementMessagesSent(String companyId, ChatChannel channel, ChatMessageSenderType senderType);
    void recordMessageContentLength(String companyId, ChatChannel channel, ChatMessageSenderType senderType, int length);
    void incrementMessagesReadByOperator(String companyId, ChatChannel channel);
    void incrementMessageStatusUpdated(String companyId, ChatChannel channel, String newStatus);

    void incrementOperatorMessagesSent(String companyId, String operatorId, ChatChannel channel);

    void incrementChatOperationError(String operationName, String companyId, String errorType);

}
