package com.example.domain.dto.ans_module.predefined_answer.response;

import java.time.Instant;

public record ApiErrorResponse(
        String message,
        String errorCode,
        Instant timeStamp,
        String path
) {}
