package com.example.domain.api.statistics_module.model.metric;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatisticsQueryRequestDTO {

    /**
     * Временной диапазон для статистики (например, "1h", "1d", "30m").
     * Используется, если startTimestamp и endTimestamp не предоставлены.
     */
    private String timeRange;

    /**
     * Необязательная начальная временная метка в эпохальных секундах.
     * Переопределяет timeRange, если предоставлены и startTimestamp, и endTimestamp.
     */
    private Long startTimestamp;

    /**
     * Необязательная конечная временная метка в эпохальных секундах.
     * Переопределяет timeRange, если предоставлены и startTimestamp, и endTimestamp.
     */
    private Long endTimestamp;

    /**
     * Необязательный шаг для запросов временных рядов (например, "15s", "1m", "1h").
     * В основном используется для базовых данных временных рядов, если они используются сводками,
     * или напрямую при запросе временных рядов.
     */
    private String step;

}
