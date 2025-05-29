package com.example.domain.api.ans_api_module.generation.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerationResponse {
    @JsonProperty("result")
    private String generatedText;

    @JsonProperty("metadata")
    private Metadata metadata;

    @Data
    public static class Metadata {
        @JsonProperty("model_name")
        private String modelName;

        @JsonProperty("generation_time_ms")
        private float generationTimeMs;

        @JsonProperty("timestamp")
        private String timestamp;

        @JsonProperty("token_count")
        private Integer tokenCount;

        @JsonProperty("device")
        private String device;
    }
}
