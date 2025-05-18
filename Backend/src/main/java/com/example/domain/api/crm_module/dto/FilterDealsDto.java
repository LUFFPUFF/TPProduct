package com.example.domain.api.crm_module.dto;

import com.example.database.model.crm_module.task.TaskPriority;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Builder;
import lombok.Data;

import java.util.List;


@Data
@Builder
public class FilterDealsDto {
    private String email;
    private Float minAmount;
    private Float maxAmount;
    private List<TaskPriority> priority;
    @Min(0)
    @Max(4)
    private Integer stage;
}
