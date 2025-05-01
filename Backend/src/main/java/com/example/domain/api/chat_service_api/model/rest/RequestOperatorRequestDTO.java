package com.example.domain.api.chat_service_api.model.rest;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RequestOperatorRequestDTO {
    @NotNull
    private Integer chatId;

    @NotNull
    private Integer clientId;
}
