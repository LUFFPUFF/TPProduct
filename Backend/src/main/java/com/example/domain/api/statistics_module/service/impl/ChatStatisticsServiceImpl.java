package com.example.domain.api.statistics_module.service.impl;

import com.example.domain.api.statistics_module.metrics.client.PrometheusQueryClient;
import com.example.domain.api.statistics_module.model.chat.ChatSummaryStatsDTO;
import com.example.domain.api.statistics_module.model.chat.MetricTimeSeriesDTO;
import com.example.domain.api.statistics_module.model.chat.StatisticsQueryRequestDTO;
import com.example.domain.api.statistics_module.model.chat.TimeSeriesDataPointDTO;
import com.example.domain.api.statistics_module.service.IChatStatisticsService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final ObjectMapper objectMapper;

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

        log.info("Requesting chat summary for company: {}, timeRange: {}", request.getCompanyId(), request.getTimeRange());

        Mono<Long> totalCreatedMono = querySingleScalar(
                String.format("sum(increase(%s%s%s))", CHATS_CREATED_TOTAL, companyFilter, rangeVectorSelector),
                "Total Created Chats"
        );
        Mono<Long> totalClosedMono = querySingleScalar(
                String.format("sum(increase(%s%s%s))", CHATS_CLOSED_TOTAL, companyFilter, rangeVectorSelector),
                "Total Closed Chats"
        );
        Mono<Long> totalMessagesMono = querySingleScalar(
                String.format("sum(increase(%s%s%s))", MESSAGES_SENT_TOTAL, companyFilter, rangeVectorSelector),
                "Total Messages Sent"
        );

        String avgDurationQuery = String.format(
                "(sum(increase(%1$s%2$s%3$s)) OR on() vector(0)) / (sum(increase(%4$s%2$s%3$s)) > 0 OR on() vector(1))",
                CHAT_DURATION_SECONDS_SUM, companyFilter, rangeVectorSelector,
                CHAT_DURATION_SECONDS_COUNT
        );

        Mono<Double> avgDurationMono = querySingleScalarDouble(avgDurationQuery, "Average Chat Duration")
                .onErrorReturn(Double.NaN);

        return Mono.zip(totalCreatedMono, totalClosedMono, totalMessagesMono, avgDurationMono)
                .map(tuple -> ChatSummaryStatsDTO.builder()
                        .companyId(request.getCompanyId())
                        .timeRange(request.getTimeRange())
                        .totalChatsCreated(tuple.getT1())
                        .totalChatsClosed(tuple.getT2())
                        .totalMessagesSent(tuple.getT3())
                        .averageChatDurationSeconds((tuple.getT4() == null || tuple.getT4().isNaN() || tuple.getT4().isInfinite()) ? null : tuple.getT4())
                        .build())
                .doOnSuccess(summary -> log.info("Built chat summary: {}", summary))
                .doOnError(e -> log.error("Error building chat summary for request {}: {}", request, e.getMessage(), e));
    }

    @Override
    public Mono<List<MetricTimeSeriesDTO>> getChatsCreatedTimeSeries(StatisticsQueryRequestDTO request) {
        String companyFilter = buildCompanyFilter(request.getCompanyId());
        long start = request.getStartTimestamp() != null ? request.getStartTimestamp() : Instant.now().minusSeconds(parseTimeRangeToSeconds(request.getTimeRange())).getEpochSecond();
        long end = request.getEndTimestamp() != null ? request.getEndTimestamp() : Instant.now().getEpochSecond();
        String step = request.getStep() != null ? request.getStep() : determineStep(start, end);

        String promql = String.format("sum by (channel) (increase(%s%s[%s]))",
                CHATS_CREATED_TOTAL,
                companyFilter,
                step
        );
        log.info("Requesting chats created time series. Query: {}, Start: {}, End: {}, Step: {}", promql, start, end, step);

        return prometheusClient.queryRange(promql, start, end, step)
                .map(this::parseRangeQueryMatrixResult)
                .doOnSuccess(result -> log.info("Chats created time series result count: {} for query: {}", result.size(), promql))
                .onErrorResume(e -> {
                    log.error("Failed to get chats created time series for query [{}]: {}", promql, e.getMessage(), e);
                    return Mono.just(new ArrayList<>());
                });
    }

    @Override
    public Mono<List<MetricTimeSeriesDTO>> getAverageChatDurationTimeSeries(StatisticsQueryRequestDTO request) {
        String companyFilter = buildCompanyFilter(request.getCompanyId());
        long start = request.getStartTimestamp() != null ? request.getStartTimestamp() : Instant.now().minusSeconds(parseTimeRangeToSeconds(request.getTimeRange())).getEpochSecond();
        long end = request.getEndTimestamp() != null ? request.getEndTimestamp() : Instant.now().getEpochSecond();
        String step = request.getStep() != null ? request.getStep() : determineStep(start, end);
        String subQueryRange = determineSubQueryRange(step);


        String promql = String.format(
                "(sum by (channel) (increase(%1$s%2$s[%3$s])) OR on(channel) vector(0)) / (sum by (channel) (increase(%4$s%2$s[%3$s])) > 0 OR on(channel) vector(1))",
                CHAT_DURATION_SECONDS_SUM, companyFilter, subQueryRange,
                CHAT_DURATION_SECONDS_COUNT
        );
        log.info("Requesting avg chat duration time series. Query: {}, Start: {}, End: {}, Step: {}, SubQueryRange: {}", promql, start, end, step, subQueryRange);

        return prometheusClient.queryRange(promql, start, end, step)
                .map(this::parseRangeQueryMatrixResult)
                .doOnSuccess(result -> log.info("Avg chat duration time series result count: {} for query: {}", result.size(), promql))
                .onErrorResume(e -> {
                    log.error("Failed to get avg chat duration time series for query [{}]: {}", promql, e.getMessage(), e);
                    return Mono.just(new ArrayList<>());
                });
    }

    private Mono<Long> querySingleScalar(String promqlQuery, String queryDescription) {
        log.debug("Executing scalar query for [{}]: {}", queryDescription, promqlQuery);
        return prometheusClient.query(promqlQuery)
                .map(jsonNode -> {
                    logPrometheusResponse(queryDescription, promqlQuery, jsonNode);
                    if (isResultEmpty(jsonNode)) {
                        log.info("Result is empty for scalar query [{}]. Query: {}", queryDescription, promqlQuery);
                        return 0L;
                    }
                    JsonNode valueNode = jsonNode.path("data").path("result").path(0).path("value").path(1);
                    if (valueNode.isMissingNode()) {
                        log.warn("ValueNode is missing in Prometheus response for scalar query [{}]. Query: {}", queryDescription, promqlQuery);
                        return 0L;
                    }
                    String textValue = valueNode.asText();
                    log.info("Extracted value string for [{}]: '{}'. Query: {}", queryDescription, textValue, promqlQuery);
                    try {
                        double doubleValue = Double.parseDouble(textValue);
                        return Math.round(doubleValue);
                    } catch (NumberFormatException e) {
                        log.error("Could not parse value '{}' to Double/Long for scalar query [{}]. Query: {}", textValue, queryDescription, promqlQuery, e);
                        return 0L;
                    }
                })
                .onErrorResume(e -> {
                    log.error("Error executing scalar query for [{}]. Query: {}: {}", queryDescription, promqlQuery, e.getMessage(), e);
                    return Mono.just(0L);
                });
    }

    private Mono<Double> querySingleScalarDouble(String promqlQuery, String queryDescription) {
        log.debug("Executing scalar double query for [{}]: {}", queryDescription, promqlQuery);
        return prometheusClient.query(promqlQuery)
                .map(jsonNode -> {
                    logPrometheusResponse(queryDescription, promqlQuery, jsonNode);
                    if (isResultEmpty(jsonNode)) {
                        log.info("Result is empty for scalar double query [{}]. Query: {}", queryDescription, promqlQuery);
                        return Double.NaN;
                    }
                    JsonNode valueNode = jsonNode.path("data").path("result").path(0).path("value").path(1);
                    if (valueNode.isMissingNode()) {
                        log.warn("ValueNode is missing for scalar double query [{}]. Query: {}", queryDescription, promqlQuery);
                        return Double.NaN;
                    }
                    String textValue = valueNode.asText();
                    log.info("Extracted value string for [{}]: '{}'. Query: {}", queryDescription, textValue, promqlQuery);
                    if ("NaN".equalsIgnoreCase(textValue) || "Inf".equalsIgnoreCase(textValue) || "+Inf".equalsIgnoreCase(textValue) || "-Inf".equalsIgnoreCase(textValue)) {
                        return Double.NaN;
                    }
                    try {
                        return Double.parseDouble(textValue);
                    } catch (NumberFormatException e) {
                        log.error("Could not parse value '{}' to Double for scalar double query [{}]. Query: {}", textValue, queryDescription, promqlQuery, e);
                        return Double.NaN;
                    }
                })
                .onErrorResume(e -> {
                    log.error("Error executing scalar double query for [{}]. Query: {}: {}",queryDescription, promqlQuery, e.getMessage(), e);
                    return Mono.just(Double.NaN);
                });
    }

    private void logPrometheusResponse(String queryDescription, String promqlQuery, JsonNode jsonNode) {
        try {
            log.debug("Prometheus response for [{}], Query [{}]: {}", queryDescription, promqlQuery, objectMapper.writeValueAsString(jsonNode));
        } catch (JsonProcessingException e) {
            log.warn("Could not serialize Prometheus response to JSON for logging. Query [{}], Description [{}], Raw: {}", promqlQuery, queryDescription, jsonNode.toString().substring(0, Math.min(jsonNode.toString().length(), 500)));
        }
    }

    private List<MetricTimeSeriesDTO> parseRangeQueryMatrixResult(JsonNode jsonNode) {
        List<MetricTimeSeriesDTO> seriesList = new ArrayList<>();
        if (jsonNode == null || !jsonNode.path("status").asText("").equals("success")) {
            log.warn("Prometheus range query was not successful or returned null. Response: {}", jsonNode != null ? jsonNode.toString().substring(0, Math.min(jsonNode.toString().length(), 500)) : "null");
            return seriesList;
        }

        JsonNode result = jsonNode.path("data").path("result");
        if (result.isMissingNode() || !result.isArray()) {
            log.warn("Prometheus range result data is missing or not an array. Response: {}", jsonNode.toString().substring(0, Math.min(jsonNode.toString().length(), 500)));
            return seriesList;
        }

        if (result.isEmpty()) {
            log.info("Prometheus range result is an empty array. No data points found.");
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
            JsonNode valuesNode = seriesNode.path("values");
            if (valuesNode.isMissingNode() || !valuesNode.isArray() || valuesNode.isEmpty()) {
                log.debug("No values found for series with labels: {}", labels);
            } else {
                valuesNode.forEach(valuePair -> {
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
                                log.warn("Could not parse metric value '{}' to double for timestamp {} and labels {}", valueStr, timestamp, labels);
                            }
                        } else {
                            log.debug("Skipping NaN/Inf value for timestamp {} and labels {}", timestamp, labels);
                        }
                    } else {
                        log.warn("Invalid value pair format: {} for labels {}", valuePair.toString().substring(0, Math.min(valuePair.toString().length(), 100)), labels);
                    }
                });
            }
            seriesDto.setValues(dataPoints);
            seriesList.add(seriesDto);
        }
        return seriesList;
    }

    private boolean isResultEmpty(JsonNode jsonNode) {
        if (jsonNode == null || !jsonNode.path("status").asText("").equals("success")) {
            log.warn("Prometheus query status not success or node is null.");
            return true;
        }
        JsonNode result = jsonNode.path("data").path("result");
        if (result.isMissingNode() || !result.isArray() || result.isEmpty()) {
            log.info("Prometheus result data is missing, not an array, or empty.");
            return true;
        }
        return false;
    }

    private String buildCompanyFilter(String companyId) {
        return (companyId == null || companyId.isBlank() || "all".equalsIgnoreCase(companyId))
                ? ""
                : String.format("{company_id=\"%s\"}", companyId);
    }

    private long parseTimeRangeToSeconds(String timeRange) {
        if (timeRange == null || timeRange.isBlank()) {
            log.warn("Time range is null or blank, defaulting to 1 hour (3600s).");
            return 3600;
        }
        try {
            if (timeRange.endsWith("h")) return Long.parseLong(timeRange.substring(0, timeRange.length() - 1)) * 3600;
            if (timeRange.endsWith("d")) return Long.parseLong(timeRange.substring(0, timeRange.length() - 1)) * 86400;
            if (timeRange.endsWith("m")) return Long.parseLong(timeRange.substring(0, timeRange.length() - 1)) * 60;
            if (timeRange.endsWith("s")) return Long.parseLong(timeRange.substring(0, timeRange.length() - 1));
            return Long.parseLong(timeRange);
        } catch (NumberFormatException e) {
            log.warn("Could not parse time range '{}', defaulting to 1 hour (3600s). Error: {}", timeRange, e.getMessage());
            return 3600;
        }
    }

    private String determineStep(long startEpochSeconds, long endEpochSeconds) {
        long durationSeconds = endEpochSeconds - startEpochSeconds;
        if (durationSeconds <= 0) return "15s";
        if (durationSeconds <= 3600) return "1m";
        if (durationSeconds <= 6 * 3600) return "5m";
        if (durationSeconds <= 12 * 3600) return "10m";
        if (durationSeconds <= 24 * 3600) return "15m";
        if (durationSeconds <= 7 * 24 * 3600) return "1h";
        if (durationSeconds <= 30 * 24 * 3600) return "6h";
        return "1d";
    }

    private String determineSubQueryRange(String step) {
        if (step.endsWith("s")) {
            long seconds = Long.parseLong(step.substring(0, step.length() - 1));
            if (seconds < 60) return "1m";
            return step;
        }
        if (step.endsWith("m")) {
            long minutes = Long.parseLong(step.substring(0, step.length() - 1));
            if (minutes < 5) return "5m";
            return step;
        }
        return step;
    }
}