package com.example.domain.api.statistics_module.metrics.service;

public interface IAnswerSearchMetricsService {

    void incrementEmptyQuery(Integer companyId, String category);

    void incrementLongQuery(Integer companyId, String category);

    void recordResultsReturned(int count, Integer companyId, String category);

    void incrementNoResultsFound(Integer companyId, String category);
}
