package com.example.domain.api.ans_api_module.correction_answer.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GenerationResponse {

    @JsonProperty("generated_text")
    private String generatedText;
}
