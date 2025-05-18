package com.example.domain.api.crm_module.dto;

import com.example.database.model.company_subscription_module.user_roles.user.User;
import com.example.database.model.crm_module.client.Client;
import com.example.database.model.crm_module.deal.DealStage;
import com.example.database.model.crm_module.deal.DealStatus;
import com.example.database.model.crm_module.task.TaskPriority;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CreateDealDtoReq {
    @NotNull
    private Integer client_id;
    private String content;
    @NotNull
    private Float amount;
    @NotNull
    private String title;
    @NotNull
    private TaskPriority priority;
    private DealStatus dealStatus;
    private DealStage dealStage;
    private Client client;
    private User user;
}
