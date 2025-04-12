package com.example.domain.api.ans_api_module.template.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
