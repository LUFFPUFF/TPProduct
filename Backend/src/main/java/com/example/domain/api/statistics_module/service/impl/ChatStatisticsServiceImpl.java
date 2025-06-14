package com.example.domain.api.statistics_module.service.impl;

import com.example.database.model.chats_messages_module.chat.ChatChannel;
import com.example.database.model.company_subscription_module.user_roles.user.User;
import com.example.domain.api.statistics_module.metrics.client.PrometheusQueryClient;
import com.example.domain.api.statistics_module.model.chat.ChannelSpecificStatsDTO;
import com.example.domain.api.statistics_module.model.chat.ChatSummaryStatsDTO;
import com.example.domain.api.statistics_module.model.metric.MetricTimeSeriesDTO;
import com.example.domain.api.statistics_module.model.metric.StatisticsQueryRequestDTO;
import com.example.domain.api.statistics_module.model.metric.TimeSeriesDataPointDTO;
import com.example.domain.api.statistics_module.service.AbstractStatisticsService;
import com.example.domain.api.statistics_module.service.IChatStatisticsService;
import com.example.domain.security.model.UserContext;
import com.example.domain.security.util.UserContextHolder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.*;

@Slf4j
@Service
public class ChatStatisticsServiceImpl extends AbstractStatisticsService implements IChatStatisticsService {

    private static final String METRIC_PREFIX = "chat_app_";
    private static final String CHATS_CREATED_TOTAL = METRIC_PREFIX + "chats_total";
    private static final String CHATS_CLOSED_TOTAL = METRIC_PREFIX + "chats_closed_total";
    private static final String MESSAGES_SENT_TOTAL = METRIC_PREFIX + "messages_sent_total";
    private static final String CHAT_DURATION_SECONDS_SUM = METRIC_PREFIX + "chat_duration_seconds_sum";
    private static final String CHAT_DURATION_SECONDS_COUNT = METRIC_PREFIX + "chat_duration_seconds_count";

    private static final String CHAT_ASSIGNMENT_TIME_SECONDS_SUM = METRIC_PREFIX + "chat_assignment_time_seconds_sum";
    private static final String CHAT_ASSIGNMENT_TIME_SECONDS_COUNT = METRIC_PREFIX + "chat_assignment_time_seconds_count";
    private static final String CHAT_FIRST_RESPONSE_TIME_SECONDS_SUM = METRIC_PREFIX + "chat_first_operator_response_time_seconds_sum";
    private static final String CHAT_FIRST_RESPONSE_TIME_SECONDS_COUNT = METRIC_PREFIX + "chat_first_operator_response_time_seconds_count";
    private static final String CURRENT_CHATS_GAUGE = METRIC_PREFIX + "current_chats_status_gauge";

    private static final String TAG_CHANNEL = "channel";
    private static final String TAG_STATUS = "status";

    public ChatStatisticsServiceImpl(PrometheusQueryClient prometheusClient, ObjectMapper objectMapper) {
        super(prometheusClient, objectMapper);
    }

    @Override
    public Mono<ChatSummaryStatsDTO> getChatSummary(StatisticsQueryRequestDTO request) {

        UserContext userContext = UserContextHolder.getRequiredContext();

        String companyFilter = buildCompanyFilter(String.valueOf(userContext.getCompanyId()));
        String rangeVectorSelector = "[" + request.getTimeRange() + "]";

        log.info("Requesting chat summary for company: {}, timeRange: {}", userContext.getCompanyId(), request.getTimeRange());

        Mono<Long> totalCreatedMono = querySingleScalarInternal(
                String.format("sum(increase(%s%s%s))", CHATS_CREATED_TOTAL, companyFilter, rangeVectorSelector),
                "Total Created Chats"
        );
        Mono<Long> totalClosedMono = querySingleScalarInternal(
                String.format("sum(round(rate(%s%s%s) * %d))", CHATS_CLOSED_TOTAL, companyFilter, rangeVectorSelector, parseTimeRangeToSeconds(request.getTimeRange())),
                "Total Closed Chats (via rate)"
        );
        Mono<Long> totalMessagesMono = querySingleScalarInternal(
                String.format("sum(increase(%s%s%s))", MESSAGES_SENT_TOTAL, companyFilter, rangeVectorSelector),
                "Total Messages Sent"
        );
        Mono<Double> avgDurationMono = queryScalarDouble(
                String.format("(sum(increase(%1$s%2$s%3$s)) OR on() vector(0)) / (sum(increase(%4$s%2$s%3$s)) > 0 OR on() vector(1))",
                        CHAT_DURATION_SECONDS_SUM, companyFilter, rangeVectorSelector, CHAT_DURATION_SECONDS_COUNT),
                "Average Chat Duration"
        ).onErrorReturn(Double.NaN);
        Mono<Double> avgAssignmentTimeMono = queryScalarDouble(
                String.format("(sum(increase(%1$s%2$s%3$s)) OR on() vector(0)) / (sum(increase(%4$s%2$s%3$s)) > 0 OR on() vector(1))",
                        CHAT_ASSIGNMENT_TIME_SECONDS_SUM, companyFilter, rangeVectorSelector, CHAT_ASSIGNMENT_TIME_SECONDS_COUNT),
                "Average Assignment Time"
        ).onErrorReturn(Double.NaN);
        Mono<Double> avgFirstResponseTimeMono = queryScalarDouble(
                String.format("(sum(increase(%1$s%2$s%3$s)) OR on() vector(0)) / (sum(increase(%4$s%2$s%3$s)) > 0 OR on() vector(1))",
                        CHAT_FIRST_RESPONSE_TIME_SECONDS_SUM, companyFilter, rangeVectorSelector, CHAT_FIRST_RESPONSE_TIME_SECONDS_COUNT),
                "Average First Operator Response Time"
        ).onErrorReturn(Double.NaN);
        Mono<Map<String, Long>> currentByStatusMono = queryVectorAndParseToMap(
                String.format("sum by (%s) (%s%s)", TAG_STATUS, CURRENT_CHATS_GAUGE, companyFilter),
                TAG_STATUS, "Current Chats By Status"
        ).onErrorReturn(Collections.emptyMap());


        String createdByChannelQuery = String.format("sum by (%s) (increase(%s%s%s))",
                TAG_CHANNEL, CHATS_CREATED_TOTAL, companyFilter, rangeVectorSelector);
        Mono<Map<String, Long>> createdByChannelMapMono = queryVectorAndParseToMap(createdByChannelQuery, TAG_CHANNEL, "Created Chats By Channel Map")
                .onErrorReturn(Collections.emptyMap());

        String messagesByChannelQuery = String.format("sum by (%s) (increase(%s%s%s))",
                TAG_CHANNEL, MESSAGES_SENT_TOTAL, companyFilter, rangeVectorSelector);
        Mono<Map<String, Long>> messagesByChannelMapMono = queryVectorAndParseToMap(messagesByChannelQuery, TAG_CHANNEL, "Messages Sent By Channel Map")
                .onErrorReturn(Collections.emptyMap());

        String avgDurationByChannelQuery = String.format(
                "(sum by (%1$s) (increase(%2$s%3$s%4$s)) OR on(%1$s) vector(0)) / (sum by (%1$s) (increase(%5$s%3$s%4$s)) > 0 OR on(%1$s) vector(1))",
                TAG_CHANNEL, CHAT_DURATION_SECONDS_SUM, companyFilter, rangeVectorSelector, CHAT_DURATION_SECONDS_COUNT
        );
        Mono<Map<String, Double>> avgDurationByChannelMapMono = queryVectorAndParseToMapDouble(avgDurationByChannelQuery, TAG_CHANNEL, "Avg Chat Duration By Channel Map")
                .onErrorReturn(Collections.emptyMap());

        String avgFirstResponseByChannelQuery = String.format(
                "(sum by (%1$s) (increase(%2$s%3$s%4$s)) OR on(%1$s) vector(0)) / (sum by (%1$s) (increase(%5$s%3$s%4$s)) > 0 OR on(%1$s) vector(1))",
                TAG_CHANNEL, CHAT_FIRST_RESPONSE_TIME_SECONDS_SUM, companyFilter, rangeVectorSelector, CHAT_FIRST_RESPONSE_TIME_SECONDS_COUNT
        );
        Mono<Map<String, Double>> avgFirstResponseByChannelMapMono = queryVectorAndParseToMapDouble(avgFirstResponseByChannelQuery, TAG_CHANNEL, "Avg First Response Time By Channel Map")
                .onErrorReturn(Collections.emptyMap());

        List<Mono<?>> monos = Arrays.asList(
                totalCreatedMono, totalClosedMono, totalMessagesMono, avgDurationMono,
                avgAssignmentTimeMono, avgFirstResponseTimeMono, currentByStatusMono,
                createdByChannelMapMono, messagesByChannelMapMono,
                avgDurationByChannelMapMono, avgFirstResponseByChannelMapMono
        );


        return Mono.zip(monos, results -> {
                    Long totalCreated = (Long) results[0];
                    Long totalClosed = (Long) results[1];
                    Long totalMessages = (Long) results[2];
                    Double avgDurationOverall = (Double) results[3];
                    Double avgAssignmentTimeOverall = (Double) results[4];
                    Double avgFirstResponseTimeOverall = (Double) results[5];
                    @SuppressWarnings("unchecked")
                    Map<String, Long> currentByStatus = (Map<String, Long>) results[6];

                    @SuppressWarnings("unchecked")
                    Map<String, Long> createdByChannelMap = (Map<String, Long>) results[7];
                    @SuppressWarnings("unchecked")
                    Map<String, Long> messagesByChannelMap = (Map<String, Long>) results[8];
                    @SuppressWarnings("unchecked")
                    Map<String, Double> avgDurationByChannelMap = (Map<String, Double>) results[9];
                    @SuppressWarnings("unchecked")
                    Map<String, Double> avgFirstResponseByChannelMap = (Map<String, Double>) results[10];


                    ChatSummaryStatsDTO summaryBuilder = ChatSummaryStatsDTO.builder()
                            .companyId(String.valueOf(userContext.getCompanyId()))
                            .timeRange(request.getTimeRange())
                            .totalChatsCreated(totalCreated)
                            .totalChatsClosed(totalClosed)
                            .totalMessagesSent(totalMessages)
                            .averageChatDurationSeconds(orNull(avgDurationOverall))
                            .averageAssignmentTimeSeconds(orNull(avgAssignmentTimeOverall))
                            .averageFirstResponseTimeSeconds(orNull(avgFirstResponseTimeOverall))
                            .currentChatsByStatus(currentByStatus.isEmpty() ? null : currentByStatus)
                            .build();

                    summaryBuilder.setTelegramStats(buildChannelStats(ChatChannel.Telegram, createdByChannelMap, messagesByChannelMap, avgDurationByChannelMap, avgFirstResponseByChannelMap));
                    summaryBuilder.setVkStats(buildChannelStats(ChatChannel.VK, createdByChannelMap, messagesByChannelMap, avgDurationByChannelMap, avgFirstResponseByChannelMap));
                    summaryBuilder.setEmailStats(buildChannelStats(ChatChannel.Email, createdByChannelMap, messagesByChannelMap, avgDurationByChannelMap, avgFirstResponseByChannelMap));
                    summaryBuilder.setWhatsAppStats(buildChannelStats(ChatChannel.WhatsApp, createdByChannelMap, messagesByChannelMap, avgDurationByChannelMap, avgFirstResponseByChannelMap));
                    summaryBuilder.setDialogXChatStats(buildChannelStats(ChatChannel.DialogX_Chat, createdByChannelMap, messagesByChannelMap, avgDurationByChannelMap, avgFirstResponseByChannelMap));

                    return summaryBuilder;
                })
                .doOnSuccess(summary -> log.info("Built chat summary: {}", summary))
                .doOnError(e -> log.error("Error building chat summary for request {}: {}", request, e.getMessage(), e));
    }

    @Override
    public Mono<List<MetricTimeSeriesDTO>> getChatsCreatedTimeSeries(StatisticsQueryRequestDTO request) {

        UserContext userContext = UserContextHolder.getRequiredContext();
        String companyFilter = buildCompanyFilter(String.valueOf(userContext.getCompanyId()));
        long start = request.getStartTimestamp() != null ? request.getStartTimestamp() : Instant.now().minusSeconds(parseTimeRangeToSeconds(request.getTimeRange())).getEpochSecond();
        long end = request.getEndTimestamp() != null ? request.getEndTimestamp() : Instant.now().getEpochSecond();
        String step = request.getStep() != null ? request.getStep() : determineStep(start, end);

        String promql = String.format("sum by (%s) (increase(%s%s[%s]))",
                TAG_CHANNEL,
                CHATS_CREATED_TOTAL,
                companyFilter,
                step
        );
        log.info("Requesting chats created time series. Query: {}, Start: {}, End: {}, Step: {}", promql, start, end, step);

        return prometheusClient.queryRange(promql, start, end, step)
                .map(this::parseRangeQueryMatrixResult) // Этот метод должен быть доступен
                .doOnSuccess(result -> log.info("Chats created time series result count: {} for query: {}", result.size(), promql))
                .onErrorResume(e -> {
                    log.error("Failed to get chats created time series for query [{}]: {}", promql, e.getMessage(), e);
                    return Mono.just(new ArrayList<>());
                });
    }


    @Override
    public Mono<List<MetricTimeSeriesDTO>> getAverageChatDurationTimeSeries(StatisticsQueryRequestDTO request) {
        UserContext userContext = UserContextHolder.getRequiredContext();
        String companyFilter = buildCompanyFilter(String.valueOf(userContext.getCompanyId()));
        long start = request.getStartTimestamp() != null ? request.getStartTimestamp() : Instant.now().minusSeconds(parseTimeRangeToSeconds(request.getTimeRange())).getEpochSecond();
        long end = request.getEndTimestamp() != null ? request.getEndTimestamp() : Instant.now().getEpochSecond();
        String step = request.getStep() != null ? request.getStep() : determineStep(start, end);
        String subQueryRange = determineSubQueryRange(step);


        String promql = String.format(
                "(sum by (%s) (increase(%s%s[%s])) OR on(%s) vector(0)) / (sum by (%s) (increase(%s%s[%s])) > 0 OR on(%s) vector(1))",
                TAG_CHANNEL, CHAT_DURATION_SECONDS_SUM, companyFilter, subQueryRange, TAG_CHANNEL,
                TAG_CHANNEL, CHAT_DURATION_SECONDS_COUNT, companyFilter, subQueryRange, TAG_CHANNEL
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

    protected Mono<Map<String, Long>> queryVectorAndParseToMap(String promqlQuery, String tagKeyForMap, String queryDescription) {
        log.info("[STAT_QUERY_MAP] Executing Vector Query for [{}]: PromQL='{}'", queryDescription, promqlQuery);
        return prometheusClient.query(promqlQuery)
                .map(jsonNode -> {
                    Map<String, Long> map = new HashMap<>();
                    String rawJsonResponse = "Failed to serialize JsonNode";
                    try {
                        rawJsonResponse = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonNode);
                    } catch (JsonProcessingException e) { }
                    log.info("[STAT_QUERY_MAP] Raw Prometheus Response for [{}], Query [{}]:\n{}", queryDescription, promqlQuery, rawJsonResponse);

                    if (isResultEmptyInternal(jsonNode)) {
                        log.warn("[STAT_QUERY_MAP] Result is considered empty for [{}]. Query: {}. Returning empty map.", queryDescription, promqlQuery);
                        return map;
                    }

                    JsonNode resultDataArray = jsonNode.path("data").path("result");
                    if (resultDataArray.isEmpty()) {
                        log.warn("[STAT_QUERY_MAP] Prometheus 'result' array is present but empty for [{}]. Query: {}. Returning empty map.", queryDescription, promqlQuery);
                        return map;
                    }

                    for (JsonNode item : resultDataArray) {
                        String key = item.path("metric").path(tagKeyForMap).asText("UNKNOWN_" + tagKeyForMap.toUpperCase());
                        JsonNode valueNode = item.path("value").path(1);
                        long count = 0;
                        if (!valueNode.isMissingNode() && valueNode.isTextual()) {
                            try {
                                count = Math.round(Double.parseDouble(valueNode.asText()));
                            } catch (NumberFormatException e) {
                                log.warn("[STAT_QUERY_MAP] Could not parse count for [{}], key '{}', value '{}'", queryDescription, key, valueNode.asText());
                            }
                        }
                        if (count > 0 || valueNode.isMissingNode() && key.equals("UNKNOWN_" + tagKeyForMap.toUpperCase())) { // Добавляем даже если count=0 для UNKNOWN
                            map.put(key, count);
                        }
                    }
                    log.info("[STAT_QUERY_MAP] Parsed map for [{}]: {}", queryDescription, map);
                    return map;
                })
                .onErrorResume(e -> {
                    log.error("[STAT_QUERY_MAP] Error executing Prometheus vector query for [{}]. Query: {}: {}", queryDescription, promqlQuery, e.getMessage(), e);
                    return Mono.just(Collections.emptyMap());
                });
    }

    protected Mono<Map<String, Double>> queryVectorAndParseToMapDouble(String promqlQuery, String tagKeyForMap, String queryDescription) {
        log.info("[STAT_QUERY_MAP_DBL] Executing Vector Query for [{}]: PromQL='{}'", queryDescription, promqlQuery);
        return prometheusClient.query(promqlQuery)
                .map(jsonNode -> {
                    Map<String, Double> map = new HashMap<>();
                    if (isResultEmptyInternal(jsonNode)) {
                        log.warn("[STAT_QUERY_MAP_DBL] Result is considered empty for [{}]. Query: {}. Returning empty map.", queryDescription, promqlQuery);
                        return map;
                    }
                    JsonNode resultDataArray = jsonNode.path("data").path("result");
                    if (resultDataArray.isEmpty()){
                        log.warn("[STAT_QUERY_MAP_DBL] Prometheus 'result' array is present but empty for [{}]. Query: {}. Returning empty map.", queryDescription, promqlQuery);
                        return map;
                    }

                    for (JsonNode item : resultDataArray) {
                        String key = item.path("metric").path(tagKeyForMap).asText("UNKNOWN_" + tagKeyForMap.toUpperCase());
                        JsonNode valueNode = item.path("value").path(1);
                        Double value = null;
                        if (!valueNode.isMissingNode() && valueNode.isTextual()) {
                            String valueStr = valueNode.asText();
                            if (!"NaN".equalsIgnoreCase(valueStr) && !"Inf".equalsIgnoreCase(valueStr) && !"-Inf".equalsIgnoreCase(valueStr)) {
                                try {
                                    value = Double.parseDouble(valueStr);
                                } catch (NumberFormatException e) {
                                    log.warn("[STAT_QUERY_MAP_DBL] Could not parse value for [{}], key '{}', value '{}'", queryDescription, key, valueStr);
                                }
                            }
                        }
                        if (value != null) {
                            map.put(key, value);
                        }
                    }
                    log.info("[STAT_QUERY_MAP_DBL] Parsed map for [{}]: {}", queryDescription, map);
                    return map;
                })
                .onErrorResume(e -> {
                    log.error("[STAT_QUERY_MAP_DBL] Error executing Prometheus vector query for [{}]. Query: {}: {}", queryDescription, promqlQuery, e.getMessage(), e);
                    return Mono.just(Collections.emptyMap());
                });
    }

    private ChannelSpecificStatsDTO buildChannelStats(ChatChannel channel,
                                                      Map<String, Long> createdMap,
                                                      Map<String, Long> messagesMap,
                                                      Map<String, Double> avgDurationMap,
                                                      Map<String, Double> avgFirstResponseMap) {
        String channelName = channel.name();
        return ChannelSpecificStatsDTO.builder()
                .chatsCreated(createdMap.getOrDefault(channelName, 0L))
                .messagesSent(messagesMap.getOrDefault(channelName, 0L))
                .averageChatDurationSeconds(orNull(avgDurationMap.get(channelName)))
                .averageFirstResponseTimeSeconds(orNull(avgFirstResponseMap.get(channelName)))
                .build();
    }

    private String buildCompanyFilter(String companyId) {
        return (companyId == null || companyId.isBlank() || "all".equalsIgnoreCase(companyId))
                ? ""
                : String.format("{company_id=\"%s\"}", companyId);
    }

    private Double orNull(Double value) {
        return (value == null || value.isNaN() || value.isInfinite()) ? null : value;
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