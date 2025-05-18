package com.example.domain.api.crm_module.dto;

import com.example.database.model.crm_module.deal.Deal;
import com.example.database.model.crm_module.task.Task;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DealTaskDto {
    private Deal deal;
    private Task task;
}
