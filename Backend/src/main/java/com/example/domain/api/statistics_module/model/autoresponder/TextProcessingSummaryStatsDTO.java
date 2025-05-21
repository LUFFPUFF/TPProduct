package com.example.domain.api.statistics_module.model.autoresponder;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TextProcessingSummaryStatsDTO {

    private String timeRange;
    private Long totalProcessQueryRequests;
    private Long totalGeneralAnswerRequests;
    private Double avgProcessQueryDurationMs;
    private Double avgGeneralAnswerDurationMs;
    private Double avgApiCallDurationMs;
    private Long totalApiCalls;
    private Long totalApiRetries;
    private Long totalEmptyApiResponses;
    private Long totalValidationFailures;
    private Long totalErrors;
}
