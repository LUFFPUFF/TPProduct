package com.example.domain.api.statistics_module.metrics.service;

public interface ITextProcessingMetricsService {

    void incrementApiRetries(String taskName, String generationType);
    void incrementEmptyApiResponse(String taskName, String generationType);
    void incrementValidationFailures(String objectClassName);
    void incrementApiClientCall(String taskName, String generationType);
}
