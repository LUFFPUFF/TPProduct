package com.example.domain.api.statistics_module.model.metric;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TimeSeriesDataPointDTO {
    private Long timestamp;
    private Double value;
}
