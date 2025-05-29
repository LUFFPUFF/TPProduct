package com.example.domain.api.ans_api_module.generation.client;

import com.example.domain.api.ans_api_module.generation.config.MLServiceConfig;
import com.example.domain.api.ans_api_module.generation.model.GenerationRequest;
import com.example.domain.api.ans_api_module.generation.model.GenerationResponse;
import com.example.domain.api.ans_api_module.generation.exception.MLException;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    //TODO увеличено до 5 минут потому что не тянет система
    private static final Duration TIMEOUT = Duration.ofMinutes(5);
    private static final String ENDPOINT = "/generate";
    private static final String AI_SERVICE_URL = "https://dialogx.ru/ai-api/";

    public TextProcessingApiClient(ObjectMapper objectMapper) {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(TIMEOUT)
                .build();

        this.objectMapper = objectMapper;
        this.generateUri = URI.create(AI_SERVICE_URL + ENDPOINT);
    }

    public GenerationResponse generateText(GenerationRequest request) {
        String requestBody;
        try {
            requestBody = objectMapper.writeValueAsString(request);
            log.debug("Sending request to AI service: {}", requestBody);
        } catch (IOException e) {
            throw new MLException("Failed to serialize request", -1, e);
        }

        HttpRequest httpRequest = createRequest(generateUri, requestBody);

        try {
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            log.debug("Received response from AI service: {}", response.body());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                GenerationResponse parsedResponse = objectMapper.readValue(response.body(), GenerationResponse.class);

                if (parsedResponse.getGeneratedText() == null) {
                    log.error("AI service returned null generated text. Full response: {}", response.body());
                    throw new MLException("AI service returned null generated text", 500);
                }

                return parsedResponse;
            } else {
                log.error("AI service error response ({}): {}", response.statusCode(), response.body());
                throw new MLException("AI service returned error: " + response.body(), response.statusCode());
            }
        } catch (IOException e) {
            log.error("Network/IO error calling AI service", e);
            throw new MLException("Network error calling AI service", -1, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Request interrupted", e);
            throw new MLException("Request interrupted", -1, e);
        }
    }

    private HttpRequest createRequest(URI uri, String requestBody) {
        //TODO увеличено до 5 минут потому что не тянет система
        return HttpRequest.newBuilder()
                .uri(uri)
                .header("Content-Type", "application/json")
                .timeout(Duration.ofMinutes(5))
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
    }

    private List<String> extractValidationErrorsFromErrorBody(String errorBody) {
        // TODO: Реализовать парсинг тела ошибки API
        return List.of("Validation error details unavailable: " + errorBody);
    }

}
