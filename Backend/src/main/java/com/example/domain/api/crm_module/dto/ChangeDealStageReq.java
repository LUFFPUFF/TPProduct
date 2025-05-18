package com.example.domain.api.crm_module.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChangeDealStageReq {
    @Min(0)
    @Max(4)
    @NotNull
    Integer stageId;
    @NotNull
    Integer dealId;
}
