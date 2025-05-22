package com.example.domain.api.statistics_module.service.impl;

import com.example.domain.api.statistics_module.model.MetricSummaryDto;
import com.example.domain.api.statistics_module.model.auth.AuthSummaryStatsDTO;
import com.example.domain.api.statistics_module.model.auth.RegistrationSummaryStatsDTO;
import com.example.domain.api.statistics_module.model.auth.RoleSummaryStatsDTO;
import com.example.domain.api.statistics_module.model.autoresponder.AnswerSearchSummaryStatsDTO;
import com.example.domain.api.statistics_module.model.autoresponder.TextProcessingSummaryStatsDTO;
import com.example.domain.api.statistics_module.model.chat.ChatSummaryStatsDTO;
import com.example.domain.api.statistics_module.model.metric.StatisticsQueryRequestDTO;
import com.example.domain.api.statistics_module.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.HashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements IStatisticsService {

    private final IAuthStatisticsService authStatisticsService;
    private final IChatStatisticsService chatStatisticsService;
    private final IRegistrationStatisticsService registrationStatisticsService;
    private final IRoleStatisticsService roleStatisticsService;
    private final IAnswerSearchStatisticsService answerSearchStatisticsService;
    private final ITextProcessingStatisticsService textProcessingStatisticsService;

    @Override
    public Mono<MetricSummaryDto> getAllMetricsSummary(StatisticsQueryRequestDTO request) {
        Mono<AuthSummaryStatsDTO> authSummaryMono = authStatisticsService.getAuthSummary(request)
                .doOnError(e -> log.error("Error fetching auth summary for request {}: {}", request, e.getMessage()))
                .onErrorResume(e -> {
                    log.warn("Returning empty AuthSummaryStatsDTO due to error: {}", e.getMessage());
                    return Mono.just(AuthSummaryStatsDTO.builder().timeRange(request.getTimeRange()).build());
                });

        Mono<ChatSummaryStatsDTO> chatSummaryMono = chatStatisticsService.getChatSummary(request)
                .doOnError(e -> log.error("Error fetching chat summary for request {}: {}", request, e.getMessage()))
                .onErrorResume(e -> {
                    log.warn("Returning empty ChatSummaryStatsDTO due to error: {}", e.getMessage());
                    return Mono.just(ChatSummaryStatsDTO.builder()
                            .timeRange(request.getTimeRange())
                            .companyId(request.getCompanyId())
                            .build());
                });

        Mono<RegistrationSummaryStatsDTO> registrationSummaryMono = registrationStatisticsService.getRegistrationSummary(request)
                .doOnError(e -> log.error("Error fetching registration summary for request {}: {}", request, e.getMessage()))
                .onErrorResume(e -> {
                    log.warn("Returning empty RegistrationSummaryStatsDTO due to error: {}", e.getMessage());
                    return Mono.just(RegistrationSummaryStatsDTO.builder().timeRange(request.getTimeRange()).build());
                });

        Mono<RoleSummaryStatsDTO> roleSummaryMono = roleStatisticsService.getRoleSummary(request)
                .doOnError(e -> log.error("Error fetching role summary for request {}: {}", request, e.getMessage()))
                .onErrorResume(e -> {
                    log.warn("Returning empty RoleSummaryStatsDTO due to error: {}", e.getMessage());
                    return Mono.just(RoleSummaryStatsDTO.builder()
                            .timeRange(request.getTimeRange())
                            .rolesAddedCount(new HashMap<>())
                            .rolesRemovedCount(new HashMap<>())
                            .build());
                });

        Mono<AnswerSearchSummaryStatsDTO> answerSearchSummaryMono = answerSearchStatisticsService.getAnswerSearchSummary(request)
                .doOnError(e -> log.error("Error fetching answer search summary for request {}: {}", request, e.getMessage(), e))
                .onErrorResume(e -> {
                    log.warn("Returning empty AnswerSearchSummaryStatsDTO due to error: {}", e.getMessage());
                    return Mono.just(AnswerSearchSummaryStatsDTO.builder().timeRange(request.getTimeRange()).companyId(request.getCompanyId()).build());
                });

        Mono<TextProcessingSummaryStatsDTO> textProcessingSummaryMono = textProcessingStatisticsService.getTextProcessingSummary(request)
                .doOnError(e -> log.error("Error fetching text processing summary for request {}: {}", request, e.getMessage(), e))
                .onErrorResume(e -> {
                    log.warn("Returning empty TextProcessingSummaryStatsDTO due to error: {}", e.getMessage());
                    return Mono.just(TextProcessingSummaryStatsDTO.builder().timeRange(request.getTimeRange()).build());
                });

        return Mono.zip(
                        authSummaryMono,
                        chatSummaryMono,
                        registrationSummaryMono,
                        roleSummaryMono,
                        answerSearchSummaryMono,
                        textProcessingSummaryMono
                ).map(tuple -> MetricSummaryDto.builder()
                        .auth(tuple.getT1())
                        .chat(tuple.getT2())
                        .registration(tuple.getT3())
                        .role(tuple.getT4())
                        .answerSearch(tuple.getT5())
                        .textProcessing(tuple.getT6())
                        .build()
                ).doOnSuccess(summary -> log.info("Successfully built combined metrics summary."))
                .doOnError(e -> log.error("Critical error zipping all metrics summaries for request {}: {}", request, e.getMessage(), e));

    }
}
