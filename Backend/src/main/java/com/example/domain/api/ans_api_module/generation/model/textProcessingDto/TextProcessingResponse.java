package com.example.domain.api.ans_api_module.generation.model.textProcessingDto;

public record TextProcessingResponse(
        String processedText,
        String message,
        Status status) {}

