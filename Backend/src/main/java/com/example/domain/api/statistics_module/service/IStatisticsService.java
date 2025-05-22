package com.example.domain.api.statistics_module.service;

import com.example.domain.api.statistics_module.model.MetricSummaryDto;
import com.example.domain.api.statistics_module.model.metric.StatisticsQueryRequestDTO;
import reactor.core.publisher.Mono;

public interface IStatisticsService {

    Mono<MetricSummaryDto> getAllMetricsSummary(StatisticsQueryRequestDTO  request);
}
