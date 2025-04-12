package com.example.domain.api.ans_api_module.template.dto.request;

import java.time.LocalDate;
import java.util.List;

public record AnswerSearchRequest(
        String query,
        Integer companyId,
        List<String> categories,
        LocalDate createdAfter,
        boolean onlyActive
) {
}
