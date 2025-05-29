package com.example.domain.api.ans_api_module.generation.config;

public record MLServiceConfig(String baseUrl) {

    public MLServiceConfig {
        if (baseUrl == null || baseUrl.isEmpty()) {
            throw new IllegalArgumentException("baseUrl is empty");
        }
    }
}
