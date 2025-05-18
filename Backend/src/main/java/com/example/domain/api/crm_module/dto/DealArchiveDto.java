package com.example.domain.api.crm_module.dto;

import com.example.database.model.crm_module.deal.DealStatus;
import com.example.database.model.crm_module.task.TaskPriority;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
public class DealArchiveDto {
    private Integer id;
    private String FIO;
    private String email;
    private String content;
    private Float amount;
    private DealStatus status;
    private LocalDateTime createdAt;
    private Integer client_id;
    private String title;
    private TaskPriority priority;
    private LocalDateTime dueDate;
}
