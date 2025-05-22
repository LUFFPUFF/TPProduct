package com.example.domain.api.statistics_module.service;

import com.example.domain.api.statistics_module.model.auth.RoleSummaryStatsDTO;
import com.example.domain.api.statistics_module.model.metric.StatisticsQueryRequestDTO;
import reactor.core.publisher.Mono;

public interface IRoleStatisticsService {

    Mono<RoleSummaryStatsDTO> getRoleSummary(StatisticsQueryRequestDTO request);
}
