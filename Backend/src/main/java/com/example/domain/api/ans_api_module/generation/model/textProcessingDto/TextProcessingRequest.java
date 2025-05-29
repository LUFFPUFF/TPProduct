package com.example.domain.api.ans_api_module.generation.model.textProcessingDto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TextProcessingRequest(
        @NotBlank @Size(max = 5000) String text) {}
