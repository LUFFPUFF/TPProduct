package com.example.domain.api.ans_api_module.correction_answer.dto.textProcessingDto;

public record TextProcessingResponse(
        String processedText,
        String message,
        Status status) {}

