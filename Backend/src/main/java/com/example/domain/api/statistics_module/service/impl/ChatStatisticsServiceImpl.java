package com.example.domain.api.statistics_module.service.impl;

import com.example.domain.api.statistics_module.metrics.client.PrometheusQueryClient;
import com.example.domain.api.statistics_module.model.chat.ChatSummaryStatsDTO;
import com.example.domain.api.statistics_module.model.chat.MetricTimeSeriesDTO;
import com.example.domain.api.statistics_module.model.chat.StatisticsQueryRequestDTO;
import com.example.domain.api.statistics_module.model.chat.TimeSeriesDataPointDTO;
import com.example.domain.api.statistics_module.service.IChatStatisticsService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatStatisticsServiceImpl implements IChatStatisticsService {

    private final PrometheusQueryClient prometheusClient;

    private static final String METRIC_PREFIX = "chat_app_";
    private static final String CHATS_CREATED_TOTAL = METRIC_PREFIX + "chats_created_total";
    private static final String CHATS_CLOSED_TOTAL = METRIC_PREFIX + "chats_closed_total";
    private static final String MESSAGES_SENT_TOTAL = METRIC_PREFIX + "messages_sent_total";
    private static final String CHAT_DURATION_SECONDS_SUM = METRIC_PREFIX + "chat_duration_seconds_sum";
    private static final String CHAT_DURATION_SECONDS_COUNT = METRIC_PREFIX + "chat_duration_seconds_count";

    @Override
    public Mono<ChatSummaryStatsDTO> getChatSummary(StatisticsQueryRequestDTO request) {
        String companyFilter = buildCompanyFilter(request.getCompanyId());
        String rangeVectorSelector = "[" + request.getTimeRange() + "]";

        Mono<Long> totalCreatedMono = querySingleScalar(
                String.format("sum(increase(%s%s%s))", CHATS_CREATED_TOTAL, companyFilter, rangeVectorSelector)
        );
        Mono<Long> totalClosedMono = querySingleScalar(
                String.format("sum(increase(%s%s%s))", CHATS_CLOSED_TOTAL, companyFilter, rangeVectorSelector)
        );
        Mono<Long> totalMessagesMono = querySingleScalar(
                String.format("sum(increase(%s%s%s))", MESSAGES_SENT_TOTAL, companyFilter, rangeVectorSelector)
        );

        String avgDurationQuery = String.format(
                "sum(increase(%s%s%s)) / sum(increase(%s%s%s))",
                CHAT_DURATION_SECONDS_SUM, companyFilter, rangeVectorSelector,
                CHAT_DURATION_SECONDS_COUNT, companyFilter, rangeVectorSelector
        );
        Mono<Double> avgDurationMono = querySingleScalarDouble(avgDurationQuery).onErrorReturn(0.0);

        return Mono.zip(totalCreatedMono, totalClosedMono, totalMessagesMono, avgDurationMono)
                .map(tuple -> ChatSummaryStatsDTO.builder()
                        .companyId(request.getCompanyId())
                        .timeRange(request.getTimeRange())
                        .totalChatsCreated(tuple.getT1())
                        .totalChatsClosed(tuple.getT2())
                        .totalMessagesSent(tuple.getT3())
                        .averageChatDurationSeconds((tuple.getT4().isNaN() || tuple.getT4().isInfinite()) ? null : tuple.getT4())
                        .build());
    }

    @Override
    public Mono<List<MetricTimeSeriesDTO>> getChatsCreatedTimeSeries(StatisticsQueryRequestDTO request) {
        String companyFilter = buildCompanyFilter(request.getCompanyId());

        String promql = String.format("sum by (channel) (increase(%s%s[%s]))",
                CHATS_CREATED_TOTAL,
                companyFilter,
                request.getStep()
        );

        long start = request.getStartTimestamp() != null ? request.getStartTimestamp() : Instant.now().minusSeconds(parseTimeRangeToSeconds(request.getTimeRange())).getEpochSecond();
        long end = request.getEndTimestamp() != null ? request.getEndTimestamp() : Instant.now().getEpochSecond();
        String step = request.getStep() != null ? request.getStep() : determineStep(start, end);

        return prometheusClient.queryRange(promql, start, end, step)
                .map(this::parseRangeQueryMatrixResult)
                .onErrorResume(e -> {
                    log.error("Failed to get chats created time series: {}", e.getMessage());
                    return Mono.just(new ArrayList<>());
                });
    }

    @Override
    public Mono<List<MetricTimeSeriesDTO>> getAverageChatDurationTimeSeries(StatisticsQueryRequestDTO request) {
        String companyFilter = buildCompanyFilter(request.getCompanyId());
        String subQueryRange = request.getStep() != null ? request.getStep() : "5m";

        String promql = String.format(
                "sum by (channel) (increase(%s%s[%s])) / sum by (channel) (increase(%s%s[%s]))",
                CHAT_DURATION_SECONDS_SUM, companyFilter, subQueryRange,
                CHAT_DURATION_SECONDS_COUNT, companyFilter, subQueryRange
        );

        long start = request.getStartTimestamp() != null ? request.getStartTimestamp() : Instant.now().minusSeconds(parseTimeRangeToSeconds(request.getTimeRange())).getEpochSecond();
        long end = request.getEndTimestamp() != null ? request.getEndTimestamp() : Instant.now().getEpochSecond();
        String step = request.getStep() != null ? request.getStep() : determineStep(start, end);

        return prometheusClient.queryRange(promql, start, end, step)
                .map(this::parseRangeQueryMatrixResult)
                .onErrorResume(e -> {
                    log.error("Failed to get avg chat duration time series: {}", e.getMessage());
                    return Mono.just(new ArrayList<>());
                });
    }

    private Mono<Long> querySingleScalar(String promqlQuery) {
        return prometheusClient.query(promqlQuery)
                .map(jsonNode -> {
                    if (isResultEmpty(jsonNode)) return 0L;
                    JsonNode valueNode = jsonNode.path("data").path("result").path(0).path("value").path(1);
                    return valueNode.isMissingNode() ? 0L : Long.parseLong(valueNode.asText());
                })
                .onErrorReturn(0L);
    }

    private Mono<Double> querySingleScalarDouble(String promqlQuery) {
        return prometheusClient.query(promqlQuery)
                .map(jsonNode -> {
                    if (isResultEmpty(jsonNode)) return 0.0;
                    JsonNode valueNode = jsonNode.path("data").path("result").path(0).path("value").path(1);
                    if (valueNode.isMissingNode() || "NaN".equalsIgnoreCase(valueNode.asText()) || "Inf".equalsIgnoreCase(valueNode.asText())) {
                        return Double.NaN;
                    }
                    return Double.parseDouble(valueNode.asText());
                })
                .onErrorReturn(Double.NaN);
    }

    private List<MetricTimeSeriesDTO> parseRangeQueryMatrixResult(JsonNode jsonNode) {
        List<MetricTimeSeriesDTO> seriesList = new ArrayList<>();
        if (jsonNode == null || !jsonNode.path("status").asText("").equals("success")) {
            log.warn("Prometheus query was not successful or returned null: {}", jsonNode);
            return seriesList;
        }

        JsonNode result = jsonNode.path("data").path("result");
        if (result.isMissingNode() || !result.isArray()) {
            log.warn("Prometheus result data is missing or not an array: {}", result);
            return seriesList;
        }

        for (JsonNode seriesNode : result) {
            Map<String, String> labels = new HashMap<>();
            seriesNode.path("metric").fields().forEachRemaining(entry ->
                    labels.put(entry.getKey(), entry.getValue().asText()));

            MetricTimeSeriesDTO seriesDto = MetricTimeSeriesDTO.builder()
                    .labels(labels)
                    .metricName(labels.getOrDefault("__name__", "timeseries_data"))
                    .build();


            List<TimeSeriesDataPointDTO> dataPoints = new ArrayList<>();
            seriesNode.path("values").forEach(valuePair -> {
                if (valuePair.isArray() && valuePair.size() == 2) {
                    long timestamp = valuePair.get(0).asLong();
                    String valueStr = valuePair.get(1).asText();
                    if (!"NaN".equalsIgnoreCase(valueStr) && !"Inf".equalsIgnoreCase(valueStr) && !"-Inf".equalsIgnoreCase(valueStr) ) {
                        try {
                            dataPoints.add(TimeSeriesDataPointDTO.builder()
                                            .timestamp(timestamp)
                                            .value(Double.parseDouble(valueStr))
                                    .build());
                        } catch (NumberFormatException e) {
                            log.warn("Could not parse metric value '{}' to double for timestamp {}", valueStr, timestamp);
                        }
                    } else {
                        log.debug("Skipping NaN/Inf value for timestamp {}", timestamp);
                    }
                }
            });
            seriesDto.setValues(dataPoints);
            seriesList.add(seriesDto);
        }
        return seriesList;
    }

    private boolean isResultEmpty(JsonNode jsonNode) {
        if (jsonNode == null || !jsonNode.path("status").asText("").equals("success")) return true;
        JsonNode result = jsonNode.path("data").path("result");
        return result.isMissingNode() || !result.isArray() || result.isEmpty();
    }

    private String buildCompanyFilter(String companyId) {
        return (companyId == null || companyId.isBlank() || "all".equalsIgnoreCase(companyId))
                ? ""
                : String.format("{company_id=\"%s\"}", companyId);
    }

    private long parseTimeRangeToSeconds(String timeRange) {
        if (timeRange == null) return 3600;
        if (timeRange.endsWith("h")) return Long.parseLong(timeRange.substring(0, timeRange.length() - 1)) * 3600;
        if (timeRange.endsWith("d")) return Long.parseLong(timeRange.substring(0, timeRange.length() - 1)) * 86400;
        if (timeRange.endsWith("m")) return Long.parseLong(timeRange.substring(0, timeRange.length() - 1)) * 60;
        try {
            return Long.parseLong(timeRange);
        } catch (NumberFormatException e) {
            return 3600;
        }
    }

    private String determineStep(long startEpochSeconds, long endEpochSeconds) {
        long durationSeconds = endEpochSeconds - startEpochSeconds;
        if (durationSeconds <= 3600) return "15s";
        if (durationSeconds <= 6 * 3600) return "1m";
        if (durationSeconds <= 24 * 3600) return "5m";
        if (durationSeconds <= 7 * 24 * 3600) return "30m";
        return "1h";
    }
}
