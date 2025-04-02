package com.example.domain.dto.ans_module.predefined_answer.request;

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
