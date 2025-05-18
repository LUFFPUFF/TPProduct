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
        log.debug("Executing scalar query for [{}]: {}", queryDescription, promqlQuery);
        return prometheusClient.query(promqlQuery)
                .map(jsonNode -> {
                    logPrometheusResponse(queryDescription, promqlQuery, jsonNode);
                    if (isResultEmptyInternal(jsonNode)) {
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

    protected void logPrometheusResponse(String queryDescription, String promqlQuery, JsonNode jsonNode) {
        try {
            log.debug("Prometheus response for [{}], Query [{}]: {}", queryDescription, promqlQuery, objectMapper.writeValueAsString(jsonNode));
        } catch (JsonProcessingException e) {
            log.warn("Could not serialize Prometheus response to JSON for logging. Query [{}], Description [{}], Raw: {}", promqlQuery, queryDescription, jsonNode.toString().substring(0, Math.min(jsonNode.toString().length(), 500)));
        }
    }

    protected boolean isResultEmptyInternal(JsonNode jsonNode) {
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
}
