package com.example.domain.api.statistics_module.controller;


import com.example.domain.api.statistics_module.model.chat.ChatSummaryStatsDTO;
import com.example.domain.api.statistics_module.model.chat.MetricTimeSeriesDTO;
import com.example.domain.api.statistics_module.model.chat.StatisticsQueryRequestDTO;
import com.example.domain.api.statistics_module.service.IChatStatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/api/statistics/chats")
@RequiredArgsConstructor
public class ChatStatisticsController {

    private final IChatStatisticsService chatStatisticsService;

    @GetMapping("/summary")
    public Mono<ResponseEntity<ChatSummaryStatsDTO>> getChatSummary(
            @RequestParam(required = false) String companyId,
            @RequestParam(defaultValue = "1h") String timeRange) {

        StatisticsQueryRequestDTO request = StatisticsQueryRequestDTO.builder()
                .companyId(companyId)
                .timeRange(timeRange)
                .build();

        return chatStatisticsService.getChatSummary(request)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @GetMapping("/created/timeseries")
    public Mono<ResponseEntity<List<MetricTimeSeriesDTO>>> getChatsCreatedTimeSeries(
            @RequestParam(required = false) String companyId,
            @RequestParam(defaultValue = "1h") String timeRange,
            @RequestParam(required = false) Long start,
            @RequestParam(required = false) Long end,
            @RequestParam(required = false) String step) {

        StatisticsQueryRequestDTO request = StatisticsQueryRequestDTO.builder()
                .companyId(companyId)
                .timeRange(timeRange)
                .startTimestamp(start)
                .endTimestamp(end)
                .step(step)
                .build();

        return chatStatisticsService.getChatsCreatedTimeSeries(request)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.ok(List.of()));
    }

    @GetMapping("/avg-duration/timeseries")
    public Mono<ResponseEntity<List<MetricTimeSeriesDTO>>> getAverageChatDurationTimeSeries(
            @RequestParam(required = false) String companyId,
            @RequestParam(defaultValue = "1h") String timeRange,
            @RequestParam(required = false) Long start,
            @RequestParam(required = false) Long end,
            @RequestParam(required = false) String step) {

        StatisticsQueryRequestDTO request = StatisticsQueryRequestDTO.builder()
                .companyId(companyId)
                .timeRange(timeRange)
                .startTimestamp(start)
                .endTimestamp(end)
                .step(step)
                .build();

        return chatStatisticsService.getAverageChatDurationTimeSeries(request)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.ok(List.of()));
    }

}
