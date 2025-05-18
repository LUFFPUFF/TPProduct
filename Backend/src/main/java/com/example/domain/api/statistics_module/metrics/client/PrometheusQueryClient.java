package com.example.domain.api.statistics_module.metrics.client;

import com.example.domain.api.statistics_module.exception.StatisticsQueryException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;
import com.fasterxml.jackson.databind.JsonNode;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

@Component
@Slf4j
public class PrometheusQueryClient {

    private final WebClient webClient;
    private final String prometheusApiUrl;

    public PrometheusQueryClient(
            WebClient.Builder webClientBuilder,
            @Value("${prometheus.api.url:http://localhost:9090/api/v1}") String prometheusApiUrl) {

        this.prometheusApiUrl = prometheusApiUrl;
        log.info("--- PrometheusQueryClient --- Initializing with prometheusApiUrl: {}", prometheusApiUrl);

        DefaultUriBuilderFactory factory = new DefaultUriBuilderFactory(prometheusApiUrl);
        factory.setEncodingMode(DefaultUriBuilderFactory.EncodingMode.URI_COMPONENT);

        this.webClient = webClientBuilder
                .uriBuilderFactory(factory)
                .build();
        log.info("--- PrometheusQueryClient --- WebClient configured with custom UriBuilderFactory (EncodingMode: {}) and base URL: {}",
                factory.getEncodingMode(), prometheusApiUrl);
    }

    public Mono<JsonNode> query(String promqlQuery) {
        String exampleUri = UriComponentsBuilder.fromHttpUrl(this.prometheusApiUrl)
                .path("/query")
                .queryParam("query", "{promql_query_placeholder}")
                .buildAndExpand(Map.of("promql_query_placeholder", promqlQuery))
                .toUriString();
        log.info("Original PromQL query: '{}', Example constructed URI for /query: {}", promqlQuery, exampleUri);


        return webClient.get()
                .uri("/query?query={promql_param}", promqlQuery)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                        clientResponse -> clientResponse.bodyToMono(String.class)
                                .flatMap(errorBody -> {
                                    log.error("Prometheus API error for /query. Status: {}, Body: {}", clientResponse.statusCode(), errorBody);
                                    return Mono.error(WebClientResponseException.create(
                                            clientResponse.statusCode().value(),
                                            clientResponse.statusCode().toString(),
                                            clientResponse.headers().asHttpHeaders(),
                                            errorBody.getBytes(StandardCharsets.UTF_8),
                                            StandardCharsets.UTF_8));
                                }))
                .bodyToMono(JsonNode.class)
                .timeout(Duration.ofSeconds(10))
                .doOnError(e -> {
                    if (!(e instanceof WebClientResponseException) && !(e instanceof StatisticsQueryException)) {
                        log.error("Error querying Prometheus for /query (after onStatus or timeout or not a WebClientResponseException): {}", e.getMessage(), e);
                    }
                })
                .onErrorMap(e -> {
                    if (e instanceof WebClientResponseException) {
                        return new StatisticsQueryException("Prometheus API returned an error for query: " + promqlQuery, e);
                    } else if (e instanceof StatisticsQueryException) {
                        return e;
                    }
                    return new StatisticsQueryException("Failed to execute Prometheus query: " + promqlQuery, e);
                });
    }

    public Mono<JsonNode> queryRange(String promqlQuery, long startEpochSeconds, long endEpochSeconds, String step) {
        String exampleUri = UriComponentsBuilder.fromHttpUrl(this.prometheusApiUrl)
                .path("/query_range")
                .queryParam("query", "{promql_query_placeholder}")
                .queryParam("start", "{start_placeholder}")
                .queryParam("end", "{end_placeholder}")
                .queryParam("step", "{step_placeholder}")
                .buildAndExpand(Map.of(
                        "promql_query_placeholder", promqlQuery,
                        "start_placeholder", String.valueOf(startEpochSeconds),
                        "end_placeholder", String.valueOf(endEpochSeconds),
                        "step_placeholder", step
                ))
                .toUriString();
        log.info("Original PromQL range query: '{}', Example URI for /query_range: {}", promqlQuery, exampleUri);

        return webClient.get()
                .uri("/query_range?query={promql_param}&start={start_param}&end={end_param}&step={step_param}",
                        promqlQuery, startEpochSeconds, endEpochSeconds, step)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                        clientResponse -> clientResponse.bodyToMono(String.class)
                                .flatMap(errorBody -> {
                                    log.error("Prometheus API range query error. Status: {}, Body: {}", clientResponse.statusCode(), errorBody);
                                    return Mono.error(WebClientResponseException.create(
                                            clientResponse.statusCode().value(),
                                            clientResponse.statusCode().toString(),
                                            clientResponse.headers().asHttpHeaders(),
                                            errorBody.getBytes(StandardCharsets.UTF_8),
                                            StandardCharsets.UTF_8));
                                }))
                .bodyToMono(JsonNode.class)
                .timeout(Duration.ofSeconds(30))
                .doOnError(e -> {
                    if (!(e instanceof WebClientResponseException) && !(e instanceof StatisticsQueryException)) {
                        log.error("Error executing Prometheus range query for /query_range (after onStatus or timeout or not a WebClientResponseException): {}", e.getMessage(), e);
                    }
                })
                .onErrorMap(e -> {
                    if (e instanceof WebClientResponseException) {
                        return new StatisticsQueryException("Prometheus API returned an error for range query: " + promqlQuery, e);
                    } else if (e instanceof StatisticsQueryException) {
                        return e;
                    }
                    return new StatisticsQueryException("Failed to execute Prometheus range query: " + promqlQuery, e);
                });
    }
}
