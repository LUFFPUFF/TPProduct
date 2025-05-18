package com.example.domain.api.statistics_module.metrics.service;

public interface IRoleMetricsService {

    void incrementRoleAddedSuccess(String roleName);
    void incrementRoleRemovedSuccess(String roleName);
    void incrementRoleOperationFailureUserNotFound(String operation, String roleName);
    void incrementRoleOperationError(String operationName, String roleName, String errorType);
}
