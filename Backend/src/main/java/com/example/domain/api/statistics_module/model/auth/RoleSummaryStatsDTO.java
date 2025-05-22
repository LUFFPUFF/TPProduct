package com.example.domain.api.statistics_module.model.auth;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class RoleSummaryStatsDTO {
    private String timeRange;
    private Map<String, Long> rolesAddedCount;
    private Map<String, Long> rolesRemovedCount;
    private long userNotFoundFailures;
    private long totalRoleOperationErrors;
}
