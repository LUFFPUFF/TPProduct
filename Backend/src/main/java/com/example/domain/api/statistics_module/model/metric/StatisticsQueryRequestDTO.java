package com.example.domain.api.statistics_module.model.metric;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StatisticsQueryRequestDTO {
    private String companyId;
    private String timeRange;
    private Long startTimestamp;
    private Long endTimestamp;
    private String step;
}
