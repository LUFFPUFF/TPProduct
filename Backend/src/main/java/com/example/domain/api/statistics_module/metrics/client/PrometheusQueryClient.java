package com.example.domain.api.statistics_module.metrics.client;

import com.example.domain.api.statistics_module.exception.StatisticsQueryException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.Duration;

@Component
@Slf4j
public class PrometheusQueryClient {

    private final WebClient webClient;

    @Value("${prometheus.api.url:http://localhost:9090/api/v1}")
    private String prometheusApiUrl;

    public PrometheusQueryClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl(prometheusApiUrl).build();
    }

    public Mono<JsonNode> query(String promqlQuery) {
        log.debug("Executing Prometheus query: {}", promqlQuery);
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/query")
                        .queryParam("query", promqlQuery)
                        .build())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .timeout(Duration.ofSeconds(10))
                .doOnError(e -> log.error("Error querying Prometheus: {}", e.getMessage(), e))
                .onErrorMap(e -> new StatisticsQueryException("Failed to execute Prometheus query: " + promqlQuery, e));
    }

    public Mono<JsonNode> queryRange(String promqlQuery, long startEpochSeconds, long endEpochSeconds, String step) {
        log.debug("Executing Prometheus range query: {} from {} to {} step {}", promqlQuery, startEpochSeconds, endEpochSeconds, step);
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/query_range")
                        .queryParam("query", promqlQuery)
                        .queryParam("start", startEpochSeconds)
                        .queryParam("end", endEpochSeconds)
                        .queryParam("step", step)
                        .build())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .timeout(Duration.ofSeconds(30))
                .doOnError(e -> log.error("Error executing Prometheus range query: {}", e.getMessage(), e))
                .onErrorMap(e -> new StatisticsQueryException("Failed to execute Prometheus range query: " + promqlQuery, e));
    }
}
