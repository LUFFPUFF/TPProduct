package com.example.domain.api.chat_service_api.model.rest;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Collection;

@Data
public class MarkMessagesAsReadRequestDTO {
    @NotNull
    private Collection<Integer> messageIds;

    @NotNull
    private Integer requestingUserId;
}
