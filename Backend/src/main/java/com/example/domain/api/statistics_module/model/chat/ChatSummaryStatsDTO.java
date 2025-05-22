package com.example.domain.api.statistics_module.model.chat;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class ChatSummaryStatsDTO {
    private String companyId;
    private String timeRange;
    private Long totalChatsCreated;
    private Long totalChatsClosed;
    private Long totalMessagesSent;
    private Double averageChatDurationSeconds;
    private Double averageAssignmentTimeSeconds;
    private Double averageFirstResponseTimeSeconds;
    private Map<String, Long> currentChatsByStatus;
    private Map<String, Long> createdChatsByChannel;
}
