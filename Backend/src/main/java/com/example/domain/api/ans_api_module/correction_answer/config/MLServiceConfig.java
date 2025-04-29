package com.example.domain.api.ans_api_module.correction_answer.config;

public record MLServiceConfig(String baseUrl) {

    public MLServiceConfig {
        if (baseUrl == null || baseUrl.isEmpty()) {
            throw new IllegalArgumentException("baseUrl is empty");
        }
    }
}
