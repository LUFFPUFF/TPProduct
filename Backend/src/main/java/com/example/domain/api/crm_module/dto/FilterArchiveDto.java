package com.example.domain.api.crm_module.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Builder
@Data
public class FilterArchiveDto {
    private LocalDateTime fromEndDateTime;
    private LocalDateTime toEndDateTime;
    private String email;

}
