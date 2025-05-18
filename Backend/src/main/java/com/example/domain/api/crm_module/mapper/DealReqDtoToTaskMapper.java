package com.example.domain.api.crm_module.mapper;

;
import com.example.database.model.crm_module.deal.Deal;
import com.example.database.model.crm_module.deal.DealStatus;
import com.example.database.model.crm_module.task.Task;
import com.example.database.model.crm_module.task.TaskStatus;
import com.example.domain.api.crm_module.dto.CreateDealDtoReq;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
@Component
public class DealReqDtoToTaskMapper {
    public Task map(CreateDealDtoReq dealDto, Deal deal) {
        Task task = new Task();
        task.setDeal(deal);
        task.setStatus(TaskStatus.OPENED);
        task.setUser(dealDto.getUser());
        task.setTitle(dealDto.getTitle());
        task.setPriority(dealDto.getPriority());
        task.setCreatedAt(LocalDateTime.now());
        return task;
    }
}
