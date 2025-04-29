package com.example.domain.api.ans_api_module.answer_finder.nlp.lemmatizer.impl;

import com.example.domain.api.ans_api_module.answer_finder.exception.NlpException;
import com.example.domain.api.ans_api_module.answer_finder.nlp.lemmatizer.Lemmatizer;
import com.example.domain.api.ans_api_module.answer_finder.nlp.lemmatizer.config.NlpProperties;
import com.example.domain.api.ans_api_module.answer_finder.nlp.lemmatizer.dto.UdpipeRequest;
import com.example.domain.api.ans_api_module.answer_finder.nlp.lemmatizer.dto.UdpipeResponse;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@RequiredArgsConstructor
@Slf4j
public class ExternalApiLemmatizer implements Lemmatizer {

    private final WebClient webClient;
    private final NlpProperties nlpProperties;
    private final Retry retry;
    private final CircuitBreaker circuitBreaker;


    @Override
    public String lemmatize(String word) throws NlpException {
        Objects.requireNonNull(word, "Word for lemmatization cannot be null");
        if (word.trim().isEmpty()) {
            return "";
        }
        List<String> lemmas = lemmatize(Collections.singletonList(word));
        if (lemmas == null || lemmas.isEmpty()) {
            throw new NlpException("UDpipe API returned empty lemma list for single word: " + word + ". Check response format or word validity.");
        }
        return lemmas.getFirst();
    }

    @Override
    public List<String> lemmatize(List<String> words) throws NlpException {
        if (words == null || words.isEmpty()) {
            log.debug("Received null or empty list of words for Udpipe lemmatization, returning empty list.");
            return Collections.emptyList();
        }

        List<String> cleanedWords = words.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(text -> !text.isEmpty())
                .toList();

        if (cleanedWords.isEmpty()) {
            log.debug("After cleaning, list of words for Udpipe lemmatization is empty, returning empty list.");
            return Collections.emptyList();
        }

        List<String> allLemmas = new ArrayList<>();
        int apiBatchLimit = nlpProperties.getApiBatchLimit();

        if (cleanedWords.size() <= apiBatchLimit) {
            String fullText  = String.join(" ", cleanedWords);
            allLemmas.addAll(processTextBatchWithApi(fullText));
        } else {
            for (int i = 0; i < cleanedWords.size(); i += apiBatchLimit) {
                int endIndex = Math.min(i + apiBatchLimit, cleanedWords.size());
                List<String> batch = cleanedWords.subList(i, endIndex);
                String fullText  = String.join(" ", batch);
                allLemmas.addAll(processTextBatchWithApi(fullText));
            }
        }

        if (allLemmas.size() != cleanedWords.size()) {
            log.warn("Mismatch between total input words ({}) and total extracted lemmas ({}) from Udpipe API response. Input words: '{}', Raw lemmas: '{}'",
                    cleanedWords.size(), allLemmas.size(), cleanedWords, allLemmas);
        }

        return allLemmas;
    }

    private List<String> processTextBatchWithApi(String text) throws NlpException {
        Objects.requireNonNull(text, "Text cannot be null for Udpipe API call");
        if (text.trim().isEmpty()) {
            log.debug("Text is empty for API batch, skipping API call.");
            return Collections.emptyList();
        }

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("model", nlpProperties.getUdpipeModel());
        formData.add("tokenizer", nlpProperties.getUdpipeTokenizer());
        formData.add("tagger", nlpProperties.getUdpipeTagger());
        formData.add("parser", nlpProperties.getUdpipeParser());
        formData.add("output", nlpProperties.getUdpipeOutput());
        formData.add("data", text);

        log.debug("Sending UDpipe API request with formData: {}", formData);

        long startTime = System.currentTimeMillis();
        String rawResponse = null;

        try {
            UdpipeResponse response = circuitBreaker.executeSupplier(() ->
                    retry.executeSupplier(() -> {
                        log.debug("Making UDpipe API call with text length {} to {}", text.length(), nlpProperties.getApiUrl());

                        Mono<UdpipeResponse> responseMono = webClient.post()
                                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                                .body(BodyInserters.fromFormData(formData))
                                .retrieve()
                                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                                        clientResponse -> clientResponse.bodyToMono(String.class)
                                                .flatMap(errorBody -> {
                                                    int statusCode = clientResponse.statusCode().value();
                                                    String errorMessage = String.format("UDpipe API returned error status %d: %s", statusCode, errorBody);
                                                    log.error("UDpipe API call failed with status {}: {}", statusCode, errorBody);
                                                    return Mono.error(new NlpException(errorMessage));
                                                })
                                )
                                .bodyToMono(UdpipeResponse.class);

                        try {
                            return responseMono.block();
                        } catch (WebClientResponseException e) {
                            log.error("WebClient HTTP error during UDpipe API call (status {}): {}", e.getStatusCode(), e.getMessage(), e);
                            String responseBody = e.getResponseBodyAsString();
                            String errorDetail = responseBody != null && !responseBody.isEmpty() ? ": " + responseBody : "";
                            throw new NlpException("UDpipe API HTTP error: " + e.getStatusCode() + errorDetail, e);
                        } catch (Exception e) {
                            log.error("Unexpected error during UDpipe API call execution: {}", e.getMessage(), e);
                            throw new NlpException("Unexpected UDpipe API call error", e);
                        }
                    })
            );

            long duration = System.currentTimeMillis() - startTime;
            log.debug("UDpipe API call completed in {} ms. Response received.", duration);

            if (response == null) {
                log.error("UDpipe API returned null response after blocking.");
                throw new NlpException("UDpipe API returned null response.");
            }

            if (response.hasError()) {
                log.error("UDpipe API returned error in response body: {}", response.getErrorMessage());
                throw new NlpException("UDpipe API returned error: " + response.getErrorMessage());
            }

            String conlluText = response.getResult();
            rawResponse = conlluText;

            if (conlluText == null || conlluText.trim().isEmpty()) {
                log.warn("UDpipe API returned empty or null 'result' field. Raw response: {}", rawResponse);
                return Collections.emptyList();
            }

            List<String> lemmas = parseLemmasFromConllu(conlluText);

            log.debug("Parsed {} lemmas from UDpipe API response for text batch.", lemmas.size());

            return lemmas;

        } catch (Exception e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            if (rawResponse != null) {
                log.error("Raw response before final failure: {}", rawResponse);
            }

            if (cause instanceof NlpException) {
                log.error("Final failure processing UDpipe API batch after retries/circuit breaker: {}", cause.getMessage(), cause);
                throw (NlpException) cause;
            } else {
                log.error("Final unexpected failure processing UDpipe API batch after retries/circuit breaker: {}", e.getMessage(), e);
                throw new NlpException("Failed to process text batch with UDpipe API", e);
            }
        }
    }

    private List<String> parseLemmasFromConllu(String conlluText) throws NlpException {
        List<String> lemmas = new ArrayList<>();

        if (conlluText == null || conlluText.trim().isEmpty()) {
            log.warn("Received null or empty CoNLL-U text for parsing.");
            return lemmas;
        }

        log.debug("Received CoNLL-U text for parsing:\n{}", conlluText);

        try (BufferedReader reader = new BufferedReader(new StringReader(conlluText))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();

                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                String[] columns = line.split("\t");
                if (columns.length > 2 && columns[0].matches("\\d+")) {
                    String lemma = columns[2];
                    lemmas.add(lemma);
                } else {
                }
            }
        } catch (IOException e) {
            log.error("Error reading CoNLL-U text during parsing", e);
            throw new NlpException("Failed to read CoNLL-U text from Udpipe API response", e);
        } catch (Exception e) {
            log.error("Failed to parse CoNLL-U response", e);
            throw new NlpException("Failed to parse Udpipe API response", e);
        }

        log.debug("Finished parsing. Total parsed lemmas: {}", lemmas.size());
        return lemmas;
    }
}
