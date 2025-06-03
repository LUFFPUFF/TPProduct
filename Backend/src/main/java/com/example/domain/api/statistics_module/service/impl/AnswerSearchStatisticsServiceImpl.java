package com.example.domain.api.statistics_module.service.impl;

import com.example.domain.api.statistics_module.metrics.client.PrometheusQueryClient;
import com.example.domain.api.statistics_module.model.autoresponder.AnswerSearchSummaryStatsDTO;
import com.example.domain.api.statistics_module.model.metric.StatisticsQueryRequestDTO;
import com.example.domain.api.statistics_module.service.AbstractStatisticsService;
import com.example.domain.api.statistics_module.service.IAnswerSearchStatisticsService;
import com.example.domain.security.model.UserContext;
import com.example.domain.security.util.UserContextHolder;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public class AnswerSearchStatisticsServiceImpl extends AbstractStatisticsService implements IAnswerSearchStatisticsService {

    private static final String METRIC_PREFIX = "answer_search_app_";

    private static final String EXECUTION_DURATION_SECONDS_SUM = METRIC_PREFIX + "execution_duration_seconds_sum";
    private static final String EXECUTION_DURATION_SECONDS_COUNT = METRIC_PREFIX + "execution_duration_seconds_count";
    private static final String REQUESTS_TOTAL = METRIC_PREFIX + "requests_total";
    private static final String EMPTY_QUERY_TOTAL = METRIC_PREFIX + "empty_query_total";
    private static final String LONG_QUERY_TOTAL = METRIC_PREFIX + "long_query_total";
    private static final String RESULTS_RETURNED_SUM = METRIC_PREFIX + "results_returned_sum";
    private static final String NO_RESULTS_FOUND_TOTAL = METRIC_PREFIX + "no_results_found_total";
    private static final String OPERATION_ERRORS_TOTAL = METRIC_PREFIX + "operation_errors_total";

    private static final String TAG_COMPANY_ID = "company_id";

    public AnswerSearchStatisticsServiceImpl(PrometheusQueryClient prometheusClient, ObjectMapper objectMapper) {
        super(prometheusClient, objectMapper);
    }

    @Override
    public Mono<AnswerSearchSummaryStatsDTO> getAnswerSearchSummary(StatisticsQueryRequestDTO request) {

        UserContext userContext = UserContextHolder.getRequiredContext();

        String range = "[" + request.getTimeRange() + "]";
        String companyFilter = buildOptionalTagFilter(String.valueOf(userContext.getCompanyId()));

        String commonFilters = buildCommonFilters(companyFilter);

        Mono<Long> totalRequestsMono = queryScalarWithFilters(REQUESTS_TOTAL, commonFilters, range, "Total Search Requests");

        Mono<Long> sumExecutionTimeMono = queryScalarWithFilters(EXECUTION_DURATION_SECONDS_SUM, commonFilters, range, "Sum Execution Time (s)");
        Mono<Long> countExecutionTimeMono = queryScalarWithFilters(EXECUTION_DURATION_SECONDS_COUNT, commonFilters, range, "Count Executions");

        Mono<Long> emptyQueryMono = queryScalarWithFilters(EMPTY_QUERY_TOTAL, commonFilters, range, "Empty Query Requests");
        Mono<Long> longQueryMono = queryScalarWithFilters(LONG_QUERY_TOTAL, commonFilters, range, "Long Query Requests");
        Mono<Long> resultsReturnedSumMono = queryScalarWithFilters(RESULTS_RETURNED_SUM, commonFilters, range, "Total Results Returned Sum");
        Mono<Long> noResultsFoundMono = queryScalarWithFilters(NO_RESULTS_FOUND_TOTAL, commonFilters, range, "Searches With No Results");
        Mono<Long> totalErrorsMono = queryScalarWithFilters(OPERATION_ERRORS_TOTAL, commonFilters, range, "Total Search Errors");


        return Mono.zip(
                        totalRequestsMono,
                        emptyQueryMono,
                        longQueryMono,
                        resultsReturnedSumMono,
                        noResultsFoundMono,
                        totalErrorsMono,
                        sumExecutionTimeMono,
                        countExecutionTimeMono
                ).map(tuple -> {
                    Long totalRequests = tuple.getT1();
                    Long sumExecutionTime = tuple.getT7();
                    long countExecutions = tuple.getT8();

                    Double avgExecutionTime = null;
                    if (countExecutions > 0) {
                        avgExecutionTime = (double) sumExecutionTime / countExecutions;
                    }

                    return AnswerSearchSummaryStatsDTO.builder()
                            .timeRange(request.getTimeRange())
                            .companyId(String.valueOf(userContext.getCompanyId()))
                            .totalRequests(totalRequests)
                            .emptyQueryRequests(tuple.getT2())
                            .longQueryRequests(tuple.getT3())
                            .totalResultsReturned(tuple.getT4())
                            .searchesWithNoResults(tuple.getT5())
                            .totalErrors(tuple.getT6())
                            .averageExecutionTimeSeconds(avgExecutionTime)
                            .build();
                })
                .doOnSuccess(summary -> log.info("Built answer search summary: {}", summary))
                .doOnError(e -> log.error("Error building answer search summary for request {}: {}", request, e.getMessage(), e));
    }

    private String buildOptionalTagFilter(String tagValue) {
        if (tagValue == null || tagValue.isBlank() || "all".equalsIgnoreCase(tagValue)) {
            return "";
        }
        return String.format("%s=\"%s\"", AnswerSearchStatisticsServiceImpl.TAG_COMPANY_ID, tagValue);
    }

    private String buildCommonFilters(String... filters) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (String filter : filters) {
            if (filter != null && !filter.isBlank()) {
                if (!first) {
                    sb.append(",");
                }
                sb.append(filter);
                first = false;
            }
        }
        if (!sb.isEmpty()) {
            return "{" + sb + "}";
        }
        return "";
    }

    protected Mono<Long> queryScalarWithFilters(String metricName, String filters, String rangeVectorSelector, String queryDescription) {
        String effectiveFilters = (filters == null) ? "" : filters;
        String promqlQuery = String.format("sum(increase(%s%s%s))", metricName, effectiveFilters, rangeVectorSelector);
        return querySingleScalarInternal(promqlQuery, queryDescription);
    }
}
