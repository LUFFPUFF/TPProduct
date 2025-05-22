package com.example.domain.api.statistics_module.metrics.service;

public interface IRegistrationMetricsService {

    void incrementUserRegistrationAttempt();
    void incrementRegistrationCodeSentSuccess();
    void incrementRegistrationCodeSentFailure();
    void incrementRegistrationCodeCheckSuccess();
    void incrementRegistrationCodeCheckFailureInvalidCode();
    void incrementRegistrationEmailExists();
    void incrementRegistrationOperationError(String operationName, String errorType);
}
