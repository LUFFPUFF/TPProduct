package com.example.domain.api.statistics_module.model.auth;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthSummaryStatsDTO {
    private String timeRange;
    private long loginSuccessCount;
    private long loginFailureUserNotFoundCount;
    private long loginFailureWrongPasswordCount;
    private long logoutSuccessCount;
    private long totalAuthErrors;
}
