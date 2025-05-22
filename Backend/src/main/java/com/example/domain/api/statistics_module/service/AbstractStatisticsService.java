package com.example.domain.api.statistics_module.service;

import com.example.domain.api.statistics_module.metrics.client.PrometheusQueryClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@RequiredArgsConstructor
public abstract class AbstractStatisticsService {

    protected final PrometheusQueryClient prometheusClient;
    protected final ObjectMapper objectMapper;

    protected Mono<Long> queryScalar(String metricName, String rangeVectorSelector, String queryDescription) {
        String promqlQuery = String.format("sum(increase(%s%s))", metricName, rangeVectorSelector);
        return querySingleScalarInternal(promqlQuery, queryDescription);
    }

    protected Mono<Long> queryTotalScalar(String metricName, String rangeVectorSelector, String queryDescription) {
        String promqlQuery = String.format("sum(increase(%s%s))", metricName, rangeVectorSelector);
        return querySingleScalarInternal(promqlQuery, queryDescription);
    }


    protected Mono<Double> queryScalarDouble(String promqlQuery, String queryDescription) {
        log.debug("Executing scalar double query for [{}]: {}", queryDescription, promqlQuery);
        return prometheusClient.query(promqlQuery)
                .map(jsonNode -> {
                    logPrometheusResponse(queryDescription, promqlQuery, jsonNode);
                    if (isResultEmptyInternal(jsonNode)) {
                        return Double.NaN;
                    }
                    JsonNode valueNode = jsonNode.path("data").path("result").path(0).path("value").path(1);
                    if (valueNode.isMissingNode()) {
                        return Double.NaN;
                    }
                    String textValue = valueNode.asText();
                    if ("NaN".equalsIgnoreCase(textValue) || "Inf".equalsIgnoreCase(textValue) || "+Inf".equalsIgnoreCase(textValue) || "-Inf".equalsIgnoreCase(textValue)) {
                        return Double.NaN;
                    }
                    try {
                        return Double.parseDouble(textValue);
                    } catch (NumberFormatException e) {
                        log.error("Could not parse value '{}' to Double for [{}]. Query: {}", textValue, queryDescription, promqlQuery, e);
                        return Double.NaN;
                    }
                })
                .onErrorResume(e -> {
                    log.error("Error executing scalar double query for [{}]. Query: {}: {}", queryDescription, promqlQuery, e.getMessage(), e);
                    return Mono.just(Double.NaN);
                });
    }

    protected Mono<Long> querySingleScalarInternal(String promqlQuery, String queryDescription) {
        log.info("[STAT_QUERY] Executing Scalar Query for [{}]: PromQL='{}'", queryDescription, promqlQuery);
        return prometheusClient.query(promqlQuery)
                .map(jsonNode -> {
                    String rawJsonResponse;
                    try {
                        rawJsonResponse = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonNode);
                    } catch (JsonProcessingException e) {
                        log.warn("[STAT_QUERY] Failed to serialize full Prometheus JSON response for [{}], Query [{}]: {}", queryDescription, promqlQuery, e.getMessage());
                        if (jsonNode != null) rawJsonResponse = jsonNode.toString(); else rawJsonResponse = "JsonNode is null";
                    }
                    log.info("[STAT_QUERY] Raw Prometheus Response for [{}], Query [{}]:\n{}", queryDescription, promqlQuery, rawJsonResponse);

                    if (isResultEmptyInternal(jsonNode)) {
                        log.warn("[STAT_QUERY] Result is considered empty for [{}]. Query: {}. Returning 0L.", queryDescription, promqlQuery);
                        return 0L;
                    }

                    JsonNode resultDataArray = jsonNode.path("data").path("result");
                    if (resultDataArray.isEmpty()) {
                        log.warn("[STAT_QUERY] Prometheus 'result' array is present but empty for [{}]. Query: {}. Returning 0L.", queryDescription, promqlQuery);
                        return 0L;
                    }

                    JsonNode firstResult = resultDataArray.path(0);
                    JsonNode valueNode = firstResult.path("value").path(1);

                    if (valueNode.isMissingNode()) {
                        log.warn("[STAT_QUERY] ValueNode ('data.result[0].value[1]') is missing in Prometheus response for [{}]. Query: {}. First result content: {}. Returning 0L.",
                                queryDescription, promqlQuery, firstResult.toString().substring(0, Math.min(firstResult.toString().length(), 200)));
                        return 0L;
                    }

                    String textValue = valueNode.asText();
                    log.info("[STAT_QUERY] Extracted textValue for [{}]: '{}'. Query: {}", queryDescription, textValue, promqlQuery);

                    try {
                        double doubleValue = Double.parseDouble(textValue);
                        long roundedValue = Math.round(doubleValue);
                        log.info("[STAT_QUERY] Parsed doubleValue: {}, Rounded longValue: {} for [{}]. Query: {}", doubleValue, roundedValue, queryDescription, promqlQuery);
                        return roundedValue;
                    } catch (NumberFormatException e) {
                        log.error("[STAT_QUERY] Could not parse textValue '{}' to Double/Long for [{}]. Query: {}. Returning 0L.", textValue, queryDescription, promqlQuery, e);
                        return 0L;
                    }
                })
                .onErrorResume(e -> {
                    log.error("[STAT_QUERY] Error executing Prometheus query for [{}]. Query: {}: {}. Returning 0L.", queryDescription, promqlQuery, e.getMessage(), e);
                    return Mono.just(0L);
                });
    }

    protected void logPrometheusResponse(String queryDescription, String promqlQuery, JsonNode jsonNode) {
        try {
            log.debug("Prometheus response for [{}], Query [{}]: {}", queryDescription, promqlQuery, objectMapper.writeValueAsString(jsonNode));
        } catch (JsonProcessingException e) {
            log.warn("Could not serialize Prometheus response to JSON for logging. Query [{}], Description [{}], Raw: {}", promqlQuery, queryDescription, jsonNode.toString().substring(0, Math.min(jsonNode.toString().length(), 500)));
        }
    }

    protected boolean isResultEmptyInternal(JsonNode jsonNode) {
        if (jsonNode == null) {
            log.warn("[STAT_QUERY_UTIL] Prometheus response JsonNode is null.");
            return true;
        }
        String status = jsonNode.path("status").asText("");
        if (!"success".equals(status)) {
            log.warn("[STAT_QUERY_UTIL] Prometheus query status not 'success', was '{}'. Full response might indicate error details.", status);
            return true;
        }
        JsonNode result = jsonNode.path("data").path("result");
        if (result.isMissingNode()) {
            log.warn("[STAT_QUERY_UTIL] Prometheus 'data.result' path is missing.");
            return true;
        }
        if(!result.isArray()){
            log.warn("[STAT_QUERY_UTIL] Prometheus 'data.result' is not an array. Type: {}", result.getNodeType());
            return true;
        }
        if (result.isEmpty()) {
            log.info("[STAT_QUERY_UTIL] Prometheus 'data.result' array is present but empty (size 0).");
            return true;
        }
        log.debug("[STAT_QUERY_UTIL] Prometheus result is not considered empty. Result array size: {}", result.size());
        return false;
    }
}
