package com.example.domain.api.statistics_module.model.chat;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class MetricTimeSeriesDTO {
    private String metricName;
    private Map<String, String> labels;
    private List<TimeSeriesDataPointDTO> values;
}
