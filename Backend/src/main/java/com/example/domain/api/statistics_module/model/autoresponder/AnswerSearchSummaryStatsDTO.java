package com.example.domain.api.statistics_module.model.autoresponder;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnswerSearchSummaryStatsDTO {
    private String timeRange;
    private String companyId;
    private String category;
    private Long totalRequests;
    private Long emptyQueryRequests;
    private Long longQueryRequests;
    private Long totalResultsReturned;
    private Long searchesWithNoResults;
    private Long totalErrors;
    private Double averageExecutionTimeSeconds;
}
