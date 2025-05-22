package com.example.domain.api.statistics_module.service;

import com.example.domain.api.statistics_module.model.auth.RegistrationSummaryStatsDTO;
import com.example.domain.api.statistics_module.model.metric.StatisticsQueryRequestDTO;
import reactor.core.publisher.Mono;

public interface IRegistrationStatisticsService {

    Mono<RegistrationSummaryStatsDTO> getRegistrationSummary(StatisticsQueryRequestDTO request);
}
