package com.example.domain.api.ans_api_module.answer_finder.nlp.hf_similarity_client;

import com.example.domain.api.ans_api_module.answer_finder.exception.EmbeddingException;
import com.example.domain.api.ans_api_module.answer_finder.nlp.hf_similarity_client.config.HfSimilarityClientConfig;
import com.example.domain.api.ans_api_module.answer_finder.nlp.hf_similarity_client.model.HfSimilarityRequest;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Slf4j
@RequiredArgsConstructor
public class HuggingFaceSentenceSimilarityClient implements SentenceSimilarityClient {

    private final WebClient webClient;
    private final HfSimilarityClientConfig apiClientConfig;
    private final Retry retry;
    private final CircuitBreaker circuitBreaker;

    @Override
    public List<Float> getSimilarities(String sourceSentence, List<String> sentences) throws EmbeddingException {
        Objects.requireNonNull(sourceSentence, "Source sentence cannot be null");
        Objects.requireNonNull(sentences, "Sentences list cannot be null");

        int batchSize = apiClientConfig.getMaxBatchSize();

        if (sentences.isEmpty()) {
            log.debug("Sentences list is empty, returning empty similarities list.");
            return Collections.emptyList();
        }

        List<Float> allSimilarities = new ArrayList<>();

        if (sentences.size() > batchSize) {
            log.debug("Splitting large sentences list of {} into batches of size {}", sentences.size(), batchSize);
            for (int i = 0; i < sentences.size(); i += batchSize) {
                int endIndex = Math.min(i + batchSize, sentences.size());
                List<String> batch = sentences.subList(i, endIndex);
                log.debug("Processing sentences batch {} (indices {} to {})", (i / batchSize) + 1, i, endIndex - 1);
                allSimilarities.addAll(callHuggingFaceSimilarityApiWithRetryAndCircuitBreaker(sourceSentence, batch));
            }
        } else {
            allSimilarities.addAll(callHuggingFaceSimilarityApiWithRetryAndCircuitBreaker(sourceSentence, sentences));
        }

        if (allSimilarities.size() != sentences.size()) {
            log.error("Mismatch between number of input sentences ({}) and received similarities ({})",
                    sentences.size(), allSimilarities.size());

            throw new EmbeddingException("Mismatch in number of returned similarities");
        }


        log.debug("Successfully received {} similarities in total.", allSimilarities.size());

        return allSimilarities;
    }

    private List<Float> callHuggingFaceSimilarityApiWithRetryAndCircuitBreaker(String sourceSentence, List<String> sentencesBatch) throws EmbeddingException {
        Objects.requireNonNull(sourceSentence, "Source sentence cannot be null for API call");
        Objects.requireNonNull(sentencesBatch, "Sentences batch cannot be null for API call");
        if (sentencesBatch.isEmpty()) {
            log.debug("Sentences batch is empty, skipping API call.");
            return Collections.emptyList();
        }

        HfSimilarityRequest requestBody = HfSimilarityRequest.from(sourceSentence, sentencesBatch);

        long startTime = System.currentTimeMillis();

        try {
            List<Float> response = circuitBreaker.executeSupplier(() ->
                    retry.executeSupplier(() -> {
                        log.debug("Making API call for similarity with source '{}' and {} sentences in batch", sourceSentence, sentencesBatch.size());
                        Mono<List<Float>> responseMono = webClient.post()
                                .bodyValue(requestBody)
                                .retrieve()
                                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                                        clientResponse -> clientResponse.bodyToMono(String.class)
                                                .flatMap(errorBody -> {
                                                    int statusCode = clientResponse.statusCode().value();
                                                    String displayErrorBody = errorBody;
                                                    if (errorBody != null && errorBody.trim().startsWith("<!DOCTYPE html>")) {
                                                        displayErrorBody = "HTML response (possibly 503 Service Unavailable)";
                                                    }
                                                    String errorMessage = String.format("Hugging Face API returned error status %d: %s", statusCode, displayErrorBody);
                                                    log.error("API call failed with status {}: {}", statusCode, displayErrorBody);
                                                    return Mono.error(new EmbeddingException(errorMessage));
                                                })
                                )
                                .bodyToMono(new ParameterizedTypeReference<List<Float>>() {});

                        try {
                            return responseMono.block();
                        } catch (WebClientResponseException e) {
                            log.error("WebClient error during API call (status {}): {}", e.getStatusCode(), e.getMessage(), e);
                            String responseBody = e.getResponseBodyAsString();
                            String errorDetail = responseBody != null && !responseBody.isEmpty() ? ": " + responseBody : "";
                            throw new EmbeddingException("WebClient error during API call: " + e.getStatusCode() + errorDetail, e);
                        } catch (Exception e) {
                            log.error("Unexpected error during API call: {}", e.getMessage(), e);
                            throw new EmbeddingException("Unexpected error during API call", e);
                        }
                    })
            );

            long duration = System.currentTimeMillis() - startTime;
            log.debug("API call for similarity batch completed in {} ms", sentencesBatch.size(), duration);

            if (response == null) {
                log.error("Hugging Face API returned null response for batch.");
                throw new EmbeddingException("Hugging Face API returned null response for batch.");
            }

            if (response.size() != sentencesBatch.size()) {
                log.warn("Number of returned scores ({}) does not match number of sentences ({}) for batch",
                        response.size(), sentencesBatch.size());
                throw new EmbeddingException(String.format("Mismatch in batch size response for batch: expected %d, got %d", sentencesBatch.size(), response.size()));
            }


            return response;

        } catch (Exception e) {
            log.error("Final failure getting similarity batch after retries/circuit breaker: {}", e.getMessage(), e);
            Throwable cause = e.getCause();
            if (cause instanceof EmbeddingException) {
                throw (EmbeddingException) cause;
            } else {
                throw new EmbeddingException("Failed to get similarity batch from Hugging Face API", e);
            }
        }
    }
}
