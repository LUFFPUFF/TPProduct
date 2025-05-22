package com.example.domain.api.statistics_module.service;

import com.example.domain.api.statistics_module.model.auth.AuthSummaryStatsDTO;
import com.example.domain.api.statistics_module.model.metric.StatisticsQueryRequestDTO;
import reactor.core.publisher.Mono;

public interface IAuthStatisticsService {

    Mono<AuthSummaryStatsDTO> getAuthSummary(StatisticsQueryRequestDTO request);
}
