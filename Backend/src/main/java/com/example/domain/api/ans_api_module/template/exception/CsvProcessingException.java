package com.example.domain.api.ans_api_module.template.exception;

import java.util.Collections;
import java.util.Map;

public class CsvProcessingException extends RuntimeException {
    private final Map<String, String> recordData;

    public CsvProcessingException(String message,
                                  Map<String, String> recordData,
                                  Throwable cause) {
        super(message, cause);
        this.recordData = recordData;
    }

    public Map<String, String> getRecordData() {
        return Collections.unmodifiableMap(recordData);
    }
}
