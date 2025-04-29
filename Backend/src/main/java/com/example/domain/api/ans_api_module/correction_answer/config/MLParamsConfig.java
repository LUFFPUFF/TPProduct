package com.example.domain.api.ans_api_module.correction_answer.config;

import lombok.Builder;
import lombok.Getter;

import java.time.Duration;

@Getter
@Builder
public class MLParamsConfig {
    @Builder.Default
    private double temperature = 0.7;

    @Builder.Default
    private int maxNewTokens = 150;

    @Builder.Default
    private double topP = 0.85;

    @Builder.Default
    private boolean doSample = false;

    @Builder.Default
    private boolean stream = false;

    @Builder.Default
    private int maxRetries = 3;

    @Builder.Default
    private Duration retryDelay = Duration.ofSeconds(2);
}
