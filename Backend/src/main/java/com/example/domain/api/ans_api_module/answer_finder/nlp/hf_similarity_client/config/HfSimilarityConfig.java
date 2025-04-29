package com.example.domain.api.ans_api_module.answer_finder.nlp.hf_similarity_client.config;

import com.example.domain.api.ans_api_module.answer_finder.exception.EmbeddingException;
import com.example.domain.api.ans_api_module.answer_finder.nlp.hf_similarity_client.HuggingFaceSentenceSimilarityClient;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.netty.channel.ChannelOption;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.Objects;

@Slf4j
@Configuration
public class HfSimilarityConfig {

    @Bean
    @Qualifier("huggingFaceSimilarityClient")
    public HuggingFaceSentenceSimilarityClient huggingFaceApiEmbeddingModel(
            WebClient.Builder webClientBuilder,
            HfSimilarityClientConfig apiClientConfig,
            RetryRegistry retryRegistry,
            CircuitBreakerRegistry circuitBreakerRegistry
    ) {
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofMillis(apiClientConfig.getReadTimeoutMillis()))
                //TODO нет подходящего метода для connectTimeout пока использую option
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, apiClientConfig.getConnectionTimeoutMillis());

        WebClient specificWebClient = webClientBuilder
                .baseUrl(apiClientConfig.getEmbeddingApiUrl())
                .defaultHeader("Authorization", "Bearer " + apiClientConfig.getApiKey())
                .defaultHeader("Content-Type", "application/json")
                .defaultHeader("Accept", "application/json")
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();

        Retry retry = retryRegistry.retry("huggingFaceEmbeddingApi", createRetryConfig());
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("huggingFaceEmbeddingApi", createCircuitBreakerConfig());

        retry.getEventPublisher()
                .onRetry(event -> log.warn("Retry attempt #{}: Calling Hugging Face Embedding API failed with: {}",
                        event.getNumberOfRetryAttempts(), Objects.requireNonNull(event.getLastThrowable()).getMessage()));

        circuitBreaker.getEventPublisher()
                .onCallNotPermitted(_ -> log.warn("CircuitBreaker 'huggingFaceEmbeddingApi' prevented calling API"))
                .onError(event -> log.error("CircuitBreaker 'huggingFaceEmbeddingApi' recorded error: {}", event.getThrowable().getMessage()))
                .onSuccess(event -> log.debug("CircuitBreaker 'huggingFaceEmbeddingApi' recorded success"))
                .onStateTransition(event -> log.info("CircuitBreaker 'huggingFaceEmbeddingApi' transitioned from {} to {}",
                        event.getStateTransition().getFromState(), event.getStateTransition().getToState()));

        return new HuggingFaceSentenceSimilarityClient(specificWebClient, apiClientConfig, retry, circuitBreaker);
    }

    @Bean
    public RetryRegistry retryRegistry() {
        return RetryRegistry.ofDefaults();
    }

    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        return CircuitBreakerRegistry.ofDefaults();
    }

    private RetryConfig createRetryConfig() {
        return RetryConfig.custom()
                .maxAttempts(3)
                .intervalFunction(IntervalFunction.ofExponentialBackoff(Duration.ofMillis(500), 2))
                .retryExceptions(EmbeddingException.class)
                .build();
    }

    private CircuitBreakerConfig createCircuitBreakerConfig() {
        return CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(60))
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(10)
                .build();
    }
}
