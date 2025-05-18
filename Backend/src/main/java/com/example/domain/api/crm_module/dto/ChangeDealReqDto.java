package com.example.domain.api.crm_module.dto;

import com.example.database.model.crm_module.task.TaskPriority;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class ChangeDealReqDto {
    String title;
    String content;
    TaskPriority priority;
    @NotNull
    Integer dealId;
}
