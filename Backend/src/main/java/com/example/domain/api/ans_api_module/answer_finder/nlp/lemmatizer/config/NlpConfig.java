package com.example.domain.api.ans_api_module.answer_finder.nlp.lemmatizer.config;

import com.example.domain.api.ans_api_module.answer_finder.exception.NlpException;
import com.example.domain.api.ans_api_module.answer_finder.nlp.lemmatizer.Lemmatizer;
import com.example.domain.api.ans_api_module.answer_finder.nlp.lemmatizer.impl.ExternalApiLemmatizer;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.netty.channel.ChannelOption;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;

@Configuration
@Slf4j
public class NlpConfig {

    public Lemmatizer localLemmatizer(NlpProperties nlpProperties) {
        log.info("Configuring LocalLemmatizer based on property 'nlp.lemmatizer.implementation=local'");

        //TODO на данный момент не поддерживается
        throw new UnsupportedOperationException("LocalLemmatizer not implemented");
    }

    @Bean("externalApiLemmatizer")
    @ConditionalOnProperty(name = "nlp.lemmatizer.implementation", havingValue = "external-api")
    public Lemmatizer externalApiLemmatizer( WebClient.Builder webClientBuilder,
                                             NlpProperties nlpProperties,
                                             RetryRegistry retryRegistry,
                                             CircuitBreakerRegistry circuitBreakerRegistry) {

        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofMillis(nlpProperties.getReadTimeoutMillis()))
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, nlpProperties.getConnectTimeoutMillis());

        WebClient specificWebClient = webClientBuilder
                .baseUrl(nlpProperties.getApiUrl())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();

        Retry lemmatizerApiRetry = retryRegistry.retry("lemmatizerApi", createLemmatizerRetryConfig());
        CircuitBreaker lemmatizerApiCircuitBreaker = circuitBreakerRegistry.circuitBreaker("lemmatizerApi",
                createLemmatizerCircuitBreakerConfig());

        lemmatizerApiRetry.getEventPublisher().onRetry(event -> log.warn("Lemmatizer Retry attempt #{}: {}", event.getNumberOfRetryAttempts(), Objects.requireNonNull(event.getLastThrowable()).getMessage()));
        lemmatizerApiCircuitBreaker.getEventPublisher().onError(event -> log.error("Lemmatizer CircuitBreaker recorded error: {}", event.getThrowable().getMessage()));
        lemmatizerApiCircuitBreaker.getEventPublisher().onCallNotPermitted(event -> log.warn("Lemmatizer CircuitBreaker prevented calling API"));

        return new ExternalApiLemmatizer(specificWebClient, nlpProperties, lemmatizerApiRetry, lemmatizerApiCircuitBreaker);
    }

    @Bean
    public RetryRegistry retryRegistry() {
        return RetryRegistry.ofDefaults();
    }

    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        return CircuitBreakerRegistry.ofDefaults();
    }

    private RetryConfig createLemmatizerRetryConfig() {
        log.info("Configuring Retry for Lemmatizer API");
        return RetryConfig.custom()
                .maxAttempts(2)
                .intervalFunction(IntervalFunction.ofExponentialBackoff(Duration.ofMillis(300), 2))
                .retryExceptions(NlpException.class, WebClientResponseException.class)
                .build();
    }

    private CircuitBreakerConfig createLemmatizerCircuitBreakerConfig() {
        log.info("Configuring CircuitBreaker for Lemmatizer API");
        return CircuitBreakerConfig.custom()
                .failureRateThreshold(60)
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(5)
                .build();
    }


}
