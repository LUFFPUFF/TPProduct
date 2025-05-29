package com.example.domain.api.ans_api_module.generation.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GenerationRequest {

    @NotBlank
    @JsonProperty("prompt")
    private String prompt;

    @NotNull
    @JsonProperty("temperature")
    private double temperature;

    @NotNull
    @JsonProperty("max_new_tokens")
    private int maxNewTokens;

    @NotNull
    @JsonProperty("top_p")
    double topP;

    @JsonProperty("do_sample")
    boolean doSample;

    @JsonProperty("stream")
    boolean stream;

    @JsonProperty("is_text_generation")
    private boolean isTextGeneration;
}
