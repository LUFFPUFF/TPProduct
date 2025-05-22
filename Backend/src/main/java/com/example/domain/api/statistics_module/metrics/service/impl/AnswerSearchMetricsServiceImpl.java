package com.example.domain.api.statistics_module.metrics.service.impl;


import com.example.domain.api.statistics_module.metrics.service.IAnswerSearchMetricsService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnswerSearchMetricsServiceImpl implements IAnswerSearchMetricsService {

    private final MeterRegistry registry;

    private static final String PREFIX = "answer_search_app_";

    private static final String TAG_COMPANY_ID = "company_id";
    private static final String TAG_CATEGORY = "category";

    @Override
    public void incrementEmptyQuery(Integer companyId, String category) {
        buildCounter(PREFIX + "empty_query_total", "Number of requests with an empty or null client query",
                companyId, category)
                .increment();
    }

    @Override
    public void incrementLongQuery(Integer companyId, String category) {
        buildCounter(PREFIX + "long_query_total", "Number of requests with a client query longer than 1000 characters",
                companyId, category)
                .increment();
    }

    @Override
    public void recordResultsReturned(int count, Integer companyId, String category) {
        buildCounter(PREFIX + "results_returned_sum", "Total number of answer items returned across all successful searches",
                companyId, category)
                .increment(count);
    }

    @Override
    public void incrementNoResultsFound(Integer companyId, String category) {
        buildCounter(PREFIX + "no_results_found_total", "Number of successful searches that returned no answer items",
                companyId, category)
                .increment();
    }

    private Counter.Builder buildCounterBase(String name, String description) {
        return Counter.builder(name)
                .description(description)
                .tag("application_component", "answer_search");
    }

    private Counter buildCounter(String name, String description, Integer companyId, String category) {
        List<Tag> tagsList = new ArrayList<>();
        tagsList.add(Tag.of(TAG_COMPANY_ID, sanitizeTagValue(companyId != null ? companyId.toString() : null)));
        tagsList.add(Tag.of(TAG_CATEGORY, sanitizeTagValue(category)));

        return buildCounterBase(name, description)
                .tags(Tags.of(tagsList))
                .register(registry);
    }

    private String sanitizeTagValue(String value) {
        if (value == null || value.isEmpty()) {
            return "unknown";
        }
        return value.replaceAll("[^a-zA-Z0-9_\\-]", "_").toLowerCase();
    }

}
