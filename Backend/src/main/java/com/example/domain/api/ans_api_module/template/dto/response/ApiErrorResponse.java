package com.example.domain.api.ans_api_module.template.dto.response;

import java.time.Instant;

public record ApiErrorResponse(
        String message,
        String errorCode,
        Instant timeStamp,
        String path
) {}
