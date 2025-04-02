package com.example.domain.dto.ans_module.predefined_answer.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Getter
@Builder
public class UploadResultResponse {
    private int processedCount;
    private int duplicatesCount;
    @Builder.Default private List<String> globalErrors = Collections.emptyList();
    @Builder.Default private Map<Integer, String> rowErrors = Collections.emptyMap();
    private String status;

    public boolean hasErrors() {
        return !globalErrors.isEmpty() || !rowErrors.isEmpty();
    }

    public int getTotalProcessed() {
        return processedCount + duplicatesCount;
    }
}
