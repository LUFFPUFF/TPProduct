package com.example.domain.api.statistics_module.model.chat;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChatsByStatusDTO {
    private String status;
    private Long count;
}
