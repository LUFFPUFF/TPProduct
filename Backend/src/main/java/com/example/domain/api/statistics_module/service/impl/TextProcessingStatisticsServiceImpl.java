package com.example.domain.api.statistics_module.service.impl;

import com.example.domain.api.statistics_module.metrics.client.PrometheusQueryClient;
import com.example.domain.api.statistics_module.model.autoresponder.TextProcessingSummaryStatsDTO;
import com.example.domain.api.statistics_module.model.metric.StatisticsQueryRequestDTO;
import com.example.domain.api.statistics_module.service.AbstractStatisticsService;
import com.example.domain.api.statistics_module.service.ITextProcessingStatisticsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
public class TextProcessingStatisticsServiceImpl extends AbstractStatisticsService implements ITextProcessingStatisticsService {

    private static final String METRIC_PREFIX = "text_processing_";

    private static final String PROCESS_QUERY_DURATION = METRIC_PREFIX + "process_query_duration_seconds";
    private static final String GENERATE_GENERAL_ANSWER_DURATION = METRIC_PREFIX + "generate_general_answer_duration_seconds";
    private static final String API_CALL_DURATION = METRIC_PREFIX + "api_call_duration_seconds";

    private static final String REQUESTS_TOTAL = METRIC_PREFIX + "requests_total";
    private static final String API_CALLS_TOTAL = METRIC_PREFIX + "api_calls_total";
    private static final String API_RETRIES_TOTAL = METRIC_PREFIX + "api_retries_total";
    private static final String EMPTY_API_RESPONSE_TOTAL = METRIC_PREFIX + "empty_api_response_total";
    private static final String VALIDATION_FAILURES_TOTAL = METRIC_PREFIX + "validation_failures_total";
    private static final String ERRORS_TOTAL = METRIC_PREFIX + "errors_total";

    private static final String TAG_METHOD_NAME = "method_name";

    public TextProcessingStatisticsServiceImpl(PrometheusQueryClient prometheusClient, ObjectMapper objectMapper) {
        super(prometheusClient, objectMapper);
    }

    @Override
    public Mono<TextProcessingSummaryStatsDTO> getTextProcessingSummary(StatisticsQueryRequestDTO request) {
        String range = "[" + request.getTimeRange() + "]";

        log.info("Requesting text processing summary. TimeRange: {}", request.getTimeRange());

        String pqFilter = buildOptionalTagFilter("TextProcessingService.processQuery");
        Mono<Long> processQueryRequestsMono = queryScalarWithFilters(REQUESTS_TOTAL, pqFilter, range, "ProcessQuery Requests");
        Mono<Double> avgProcessQueryDurationMono = queryAverageDuration(PROCESS_QUERY_DURATION, pqFilter, range, "Avg ProcessQuery Duration");

        String ggaFilter = buildOptionalTagFilter("TextProcessingService.generateGeneralAnswer");
        Mono<Long> generalAnswerRequestsMono = queryScalarWithFilters(REQUESTS_TOTAL, ggaFilter, range, "GeneralAnswer Requests");
        Mono<Double> avgGeneralAnswerDurationMono = queryAverageDuration(GENERATE_GENERAL_ANSWER_DURATION, ggaFilter, range, "Avg GeneralAnswer Duration");

        Mono<Double> avgApiCallDurationMono = queryAverageDuration(API_CALL_DURATION, "", range, "Avg API Call Duration");
        Mono<Long> totalApiCallsMono = queryScalarWithFilters(API_CALLS_TOTAL, "", range, "Total API Calls");
        Mono<Long> totalApiRetriesMono = queryScalarWithFilters(API_RETRIES_TOTAL, "", range, "Total API Retries");
        Mono<Long> totalEmptyApiResponsesMono = queryScalarWithFilters(EMPTY_API_RESPONSE_TOTAL, "", range, "Total Empty API Responses");
        Mono<Long> totalValidationFailuresMono = queryScalarWithFilters(VALIDATION_FAILURES_TOTAL, "", range, "Total Validation Failures");
        Mono<Long> totalErrorsMono = queryScalarWithFilters(ERRORS_TOTAL, "", range, "Total Errors");

        List<Mono<?>> monos = Arrays.asList(
                processQueryRequestsMono, avgProcessQueryDurationMono,
                generalAnswerRequestsMono, avgGeneralAnswerDurationMono,
                avgApiCallDurationMono, totalApiCallsMono, totalApiRetriesMono,
                totalEmptyApiResponsesMono, totalValidationFailuresMono, totalErrorsMono
        );

        return Mono.zip(monos, combin -> {

                    Long totalProcessQueryRequests   = (Long) combin[0];
                    Double avgProcessQueryDuration   = (Double) combin[1];
                    Long totalGeneralAnswerRequests  = (Long) combin[2];
                    Double avgGeneralAnswerDuration  = (Double) combin[3];
                    Double avgApiCallDuration        = (Double) combin[4];
                    Long totalApiCalls               = (Long) combin[5];
                    Long totalApiRetries             = (Long) combin[6];
                    Long totalEmptyApiResponses      = (Long) combin[7];
                    Long totalValidationFailures     = (Long) combin[8];
                    Long totalErrors                 = (Long) combin[9];

                    return TextProcessingSummaryStatsDTO.builder()
                            .timeRange(request.getTimeRange())
                            .totalProcessQueryRequests(totalProcessQueryRequests)
                            .avgProcessQueryDurationMs(avgProcessQueryDuration != null ? avgProcessQueryDuration * 1000 : null)
                            .totalGeneralAnswerRequests(totalGeneralAnswerRequests)
                            .avgGeneralAnswerDurationMs(avgGeneralAnswerDuration != null ? avgGeneralAnswerDuration * 1000 : null)
                            .avgApiCallDurationMs(avgApiCallDuration != null ? avgApiCallDuration * 1000 : null)
                            .totalApiCalls(totalApiCalls)
                            .totalApiRetries(totalApiRetries)
                            .totalEmptyApiResponses(totalEmptyApiResponses)
                            .totalValidationFailures(totalValidationFailures)
                            .totalErrors(totalErrors)
                            .build();
                })
                .doOnSuccess(summary -> log.info("Built text processing summary: {}", summary))
                .doOnError(e -> log.error("Error building text processing summary for request {}: {}", request, e.getMessage(), e));
    }

    protected Mono<Long> queryScalarWithFilters(String metricName, String filters, String rangeVectorSelector, String queryDescription) {
        String effectiveFilters = (filters == null || filters.isBlank()) ? "" : "{" + filters + "}";
        String promqlQuery = String.format("sum(increase(%s%s%s))", metricName, effectiveFilters, rangeVectorSelector);
        return querySingleScalarInternal(promqlQuery, queryDescription);
    }

    protected Mono<Double> queryAverageDuration(String metricBaseName, String filters, String rangeVectorSelector, String queryDescription) {
        String effectiveFilters = (filters == null || filters.isBlank()) ? "" : "{" + filters + "}";
        String sumQuery = String.format("sum(increase(%s_sum%s%s))", metricBaseName, effectiveFilters, rangeVectorSelector);
        String countQuery = String.format("sum(increase(%s_count%s%s))", metricBaseName, effectiveFilters, rangeVectorSelector);

        String promqlQuery = String.format(
                "(%s > 0 OR on() vector(0)) / (%s > 0 OR on() vector(1))",
                sumQuery, countQuery
        );
        return queryScalarDouble(promqlQuery, queryDescription)
                .mapNotNull(val -> (val == null || val.isNaN() || val.isInfinite()) ? null : val);
    }

    private String buildOptionalTagFilter(String tagValue) {
        if (tagValue == null || tagValue.isBlank() || "all".equalsIgnoreCase(tagValue)) {
            return "";
        }
        return String.format("%s=\"%s\"", TextProcessingStatisticsServiceImpl.TAG_METHOD_NAME, tagValue);
    }

}
