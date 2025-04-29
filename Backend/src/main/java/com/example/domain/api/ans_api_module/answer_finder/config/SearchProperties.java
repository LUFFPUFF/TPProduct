package com.example.domain.api.ans_api_module.answer_finder.config;

import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Configuration
@ConfigurationProperties(prefix = "answersearch.weights")
@Data
@Validated
public class SearchProperties {

    @Min(0)
    private double similarity;

    @Min(0)
    private double trust;

    @Min(1)
    private int maxResults = 10;

    private double minCombinedScoreThreshold = 0.0;
}
