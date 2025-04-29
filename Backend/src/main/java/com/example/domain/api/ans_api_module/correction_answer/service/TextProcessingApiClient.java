package com.example.domain.api.ans_api_module.correction_answer.service;

import com.example.domain.api.ans_api_module.correction_answer.config.MLServiceConfig;
import com.example.domain.api.ans_api_module.correction_answer.dto.GenerationRequest;
import com.example.domain.api.ans_api_module.correction_answer.dto.GenerationResponse;
import com.example.domain.api.ans_api_module.correction_answer.exception.MLException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

@Slf4j
@Component
public class TextProcessingApiClient {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final URI generateUri;

    private static final Duration TIMEOUT = Duration.ofSeconds(5);
    private static final String ENDPOINT = "/generate";

    public TextProcessingApiClient(MLServiceConfig config, ObjectMapper objectMapper) {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(TIMEOUT)
                .build();

        this.objectMapper = objectMapper;
        this.generateUri = URI.create(config.baseUrl() + ENDPOINT);
    }

    public GenerationResponse generateText(GenerationRequest request) {
        String requestBody;
        try {
            requestBody = objectMapper.writeValueAsString(request);
        } catch (IOException e) {
            throw new MLException("Failed to serialize request", -1, e);
        }

        HttpRequest httpRequest = createRequest(generateUri, requestBody);

        try {
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return objectMapper.readValue(response.body(), GenerationResponse.class);
            } else {
                String errorBody = response.body();

                if (response.statusCode() >= 400 || response.statusCode() >= 500) {
                    List<String> apiValidationErrors = extractValidationErrorsFromErrorBody(errorBody);
                    if (!apiValidationErrors.isEmpty()) {
                        log.debug("API Validation Error ({}): {}", response.statusCode(), errorBody);
                        throw new MLException("API validation failed", response.statusCode(), apiValidationErrors);
                    }
                }
                log.error("API returned non-2xx status code {}: {}", response.statusCode(), errorBody);
                throw new MLException("API request failed with status code " + response.statusCode(), response.statusCode());
            }
        } catch (IOException e) {
            log.error("Network or IO error during API request: {}", e.getMessage(), e);
            throw new MLException("Network or IO error", -1, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("API Request Interrupted: {}", e.getMessage(), e);
            throw new MLException("API request interrupted", -1, e);
        } catch (Exception e) {
            log.error("Unexpected error during API request processing: {}", e.getMessage(), e);
            if (e instanceof com.fasterxml.jackson.core.JsonProcessingException) {
                throw new MLException("Failed to parse API response", -1, e);
            }
            throw new MLException("Unexpected error calling API", -1, e);
        }

    }

    private HttpRequest createRequest(URI uri, String requestBody) {
        return HttpRequest.newBuilder()
                .uri(uri)
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(15))
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
    }

    private List<String> extractValidationErrorsFromErrorBody(String errorBody) {
        // TODO: Реализовать парсинг тела ошибки API
        return List.of("Validation error details unavailable: " + errorBody);
    }

}
