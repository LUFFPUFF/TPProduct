package com.example.domain.api.ans_api_module.answer_finder.nlp.hf_similarity_client.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Configuration
@ConfigurationProperties(prefix = "nlp.api.client")
@Validated
@Data
public class HfSimilarityClientConfig {

    @NotBlank
    private String embeddingApiUrl;

    private String apiKey;

    private int connectionTimeoutMillis = 5000;
    private int readTimeoutMillis = 10000;
    private int maxBatchSize = 50;
}
