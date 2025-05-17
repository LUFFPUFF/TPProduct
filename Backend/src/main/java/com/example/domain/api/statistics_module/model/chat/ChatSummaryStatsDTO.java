package com.example.domain.api.statistics_module.model.chat;

import lombok.Builder;
import lombok.Data;

import java.util.List;

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
    private List<ChatsByStatusDTO> currentChatsByStatus;
    private List<ChatsByChannelDTO> createdChatsByChannel;
}
