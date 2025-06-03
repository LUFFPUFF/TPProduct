package com.example.domain.api.statistics_module.model.chat;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChannelSpecificStatsDTO {
    private Long chatsCreated;
    private Long messagesSent;
    private Double averageChatDurationSeconds;
    private Double averageFirstResponseTimeSeconds;
}
