package com.example.domain.api.ans_api_module.correction_answer.config;

import com.example.domain.api.ans_api_module.correction_answer.config.promt.PromptConfig;
import com.example.domain.api.ans_api_module.correction_answer.service.TextProcessingApiClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
@Slf4j
@RequiredArgsConstructor
public class TextProcessingConfig {

    private final Environment environment;

    @Bean
    @ConfigurationProperties(prefix = "ml.service")
    public MLServiceConfig mlServiceConfig() {
        String baseUrl = environment.getProperty("ml.service.base-url");
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalStateException("ML service base URL is not configured");
        }
        return new MLServiceConfig(baseUrl);
    }

    @Bean
    @ConfigurationProperties(prefix = "ml.prompts")
    public PromptConfig promptConfig() {
        return PromptConfig.getDefault()
                .withTemplates(
                        environment.getProperty("ml.prompts.correction-template"),
                        environment.getProperty("ml.prompts.rewrite-instruction"),
                        environment.getProperty("ml.prompts.rewrite-template")
                );
    }

    @Bean
    @ConfigurationProperties(prefix = "ml.params")
    public MLParamsConfig mlParamsConfig() {
        return new MLParamsConfig.MLParamsConfigBuilder()
                .temperature(Double.parseDouble(environment.getProperty("ml.params.temperature", "0.7")))
                .maxNewTokens(Integer.parseInt(environment.getProperty("ml.params.max-new-tokens", "10")))
                .topP(Double.parseDouble(environment.getProperty("ml.params.top-p", "0.7")))
                .doSample(Boolean.parseBoolean(environment.getProperty("ml.params.do-sample", "false")))
                .stream(Boolean.parseBoolean(environment.getProperty("ml.params.stream", "false")))
                .build();
    }

    @Bean
    public TextProcessingApiClient textProcessingApiClient(
            MLServiceConfig config,
            ObjectMapper objectMapper) {

        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

        return new TextProcessingApiClient(config, objectMapper);
    }

}
