package com.example.domain.api.statistics_module.controller;


import com.example.domain.api.statistics_module.model.chat.ChatSummaryStatsDTO;
import com.example.domain.api.statistics_module.model.chat.MetricTimeSeriesDTO;
import com.example.domain.api.statistics_module.model.chat.StatisticsQueryRequestDTO;
import com.example.domain.api.statistics_module.service.IChatStatisticsService;
import com.example.domain.security.model.UserContext;
import com.example.domain.security.service.UserContextLoader;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/api/statistics/chats")
@RequiredArgsConstructor
public class ChatStatisticsController {

    private final IChatStatisticsService chatStatisticsService;
    private final UserContextLoader userContextLoader;

    @GetMapping("/summary")
    public Mono<ResponseEntity<?>> getChatSummary(@RequestParam(required = false) String requestedCompanyIdParam,
                                                  @RequestParam(defaultValue = "1h") String timeRange,
                                                  Authentication authentication) {

        UserContext currentUserContext;
        try {
            Authentication originalAuth = SecurityContextHolder.getContext().getAuthentication();
            SecurityContextHolder.getContext().setAuthentication(authentication);
            currentUserContext = userContextLoader.loadUserContext();
            SecurityContextHolder.getContext().setAuthentication(originalAuth);
            if (originalAuth == null) SecurityContextHolder.clearContext();
        } catch (Exception e) {
            return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"error\": \"Failed to load user context\"}"));
        }

        if (currentUserContext == null || currentUserContext.getCompanyId() == null) {
            return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("{\"error\": \"User is not associated with any company.\"}"));
        }

        String targetCompanyIdForQuery;

        if (requestedCompanyIdParam != null && !requestedCompanyIdParam.isEmpty()) {
            if (!currentUserContext.getCompanyId().toString().equals(requestedCompanyIdParam)) {
                return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("{\"error\": \"Access denied to statistics for the requested company.\"}"));
            }
            targetCompanyIdForQuery = requestedCompanyIdParam;
        } else {
            targetCompanyIdForQuery = currentUserContext.getCompanyId().toString();
        }

        StatisticsQueryRequestDTO request = StatisticsQueryRequestDTO.builder()
                .companyId(targetCompanyIdForQuery)
                .timeRange(timeRange)
                .build();

        return chatStatisticsService.getChatSummary(request)
                .map(summaryDto -> ResponseEntity.ok((Object) summaryDto))
                .defaultIfEmpty(ResponseEntity.notFound().build())
                .map(response -> response);
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
