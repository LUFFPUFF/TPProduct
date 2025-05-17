package com.example.domain.api.statistics_module.service;

import com.example.domain.api.statistics_module.model.chat.ChatSummaryStatsDTO;
import com.example.domain.api.statistics_module.model.chat.MetricTimeSeriesDTO;
import com.example.domain.api.statistics_module.model.chat.StatisticsQueryRequestDTO;
import reactor.core.publisher.Mono;

import java.util.List;

public interface IChatStatisticsService {

    Mono<ChatSummaryStatsDTO> getChatSummary(StatisticsQueryRequestDTO request);

    Mono<List<MetricTimeSeriesDTO>> getChatsCreatedTimeSeries(StatisticsQueryRequestDTO request);

    Mono<List<MetricTimeSeriesDTO>> getAverageChatDurationTimeSeries(StatisticsQueryRequestDTO request);
}
