package com.example.domain.api.statistics_module.metrics.service;

public interface IAuthMetricsService {

    void incrementLoginSuccess();
    void incrementLoginFailureUserNotFound();
    void incrementLoginFailureWrongPassword();
    void incrementLogoutSuccess();
    void incrementAuthOperationError(String operationName, String errorType);
}
