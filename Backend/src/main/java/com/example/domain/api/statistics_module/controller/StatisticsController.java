package com.example.domain.api.statistics_module.controller;

import com.example.domain.api.statistics_module.model.MetricSummaryDto;
import com.example.domain.api.statistics_module.model.metric.StatisticsQueryRequestDTO;
import com.example.domain.api.statistics_module.service.IStatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.Instant;

@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
public class StatisticsController {

    private final IStatisticsService statisticsService;

    @GetMapping("/summary")
    public Mono<ResponseEntity<MetricSummaryDto>> getAllMetricsSummary(
            @RequestParam(name = "timeRange", defaultValue = "1h") String timeRange,
            @RequestParam(name = "companyId", required = false) String companyId,
            @RequestParam(name = "startTimestamp", required = false) Long startTimestamp,
            @RequestParam(name = "endTimestamp", required = false) Long endTimestamp,
            @RequestParam(name = "step", required = false) String step
    ) {
        StatisticsQueryRequestDTO requestBuilder = StatisticsQueryRequestDTO.builder()
                .timeRange(timeRange)
                .companyId(companyId)
                .step(step)
                .build();

        if (startTimestamp != null && endTimestamp != null) {
            requestBuilder.setStartTimestamp(startTimestamp);
            requestBuilder.setEndTimestamp(endTimestamp);
        } else if (startTimestamp != null) {
            requestBuilder.setStartTimestamp(startTimestamp);
            requestBuilder.setEndTimestamp(Instant.now().getEpochSecond());
        }

        return statisticsService.getAllMetricsSummary(requestBuilder)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }
}
