package com.example.domain.api.statistics_module.service;

import com.example.domain.api.statistics_module.model.autoresponder.AnswerSearchSummaryStatsDTO;
import com.example.domain.api.statistics_module.model.metric.StatisticsQueryRequestDTO;
import reactor.core.publisher.Mono;

public interface IAnswerSearchStatisticsService {

    Mono<AnswerSearchSummaryStatsDTO> getAnswerSearchSummary(StatisticsQueryRequestDTO request);
}
