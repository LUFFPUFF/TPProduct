package com.example.domain.api.crm_module.mapper;

import com.example.database.model.crm_module.deal.Deal;
import com.example.database.model.crm_module.task.Task;
import com.example.domain.api.crm_module.dto.DealDto;
import org.mapstruct.Mapper;

@Mapper
public class DealToDealDtoMapper {
    public DealDto map(Deal deal, Task task) {
        return DealDto.builder()
                .id(deal.getId())
                .FIO(deal.getUser().getFullName())
                .client_id(deal.getClient().getId())
                .email(deal.getUser().getEmail())
                .title(task.getTitle())
                .stageId(deal.getStage().getId())
                .amount(deal.getAmount())
                .content(deal.getContent())
                .createdAt(deal.getCreatedAt())
                .priority(task.getPriority())
                .status(deal.getStatus())
                .build();
    }
}
