package com.example.domain.api.statistics_module.service;

import com.example.domain.api.statistics_module.model.autoresponder.TextProcessingSummaryStatsDTO;
import com.example.domain.api.statistics_module.model.metric.StatisticsQueryRequestDTO;
import reactor.core.publisher.Mono;

public interface ITextProcessingStatisticsService {

    Mono<TextProcessingSummaryStatsDTO> getTextProcessingSummary(StatisticsQueryRequestDTO request);
}
