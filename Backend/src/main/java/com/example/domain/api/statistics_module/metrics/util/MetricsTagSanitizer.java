package com.example.domain.api.statistics_module.metrics.util;

import org.springframework.util.StringUtils;

public final class MetricsTagSanitizer {

    private static final String DEFAULT_TAG_VALUE = "unknown";
    private static final int MAX_TAG_VALUE_LENGTH = 250;

    private MetricsTagSanitizer() {

    }

    public static String sanitize(String value) {
        if (!StringUtils.hasText(value)) {
            return DEFAULT_TAG_VALUE;
        }

        String sanitizedValue = value.toLowerCase();

        sanitizedValue = sanitizedValue.replaceAll("\\s+", "_");
        sanitizedValue = sanitizedValue.replaceAll("[.-]", "_");

        sanitizedValue = sanitizedValue.replaceAll("[^a-z0-9_]", "");

        if (!StringUtils.hasText(sanitizedValue)) {
            return DEFAULT_TAG_VALUE;
        }

        if (sanitizedValue.length() > MAX_TAG_VALUE_LENGTH) {
            sanitizedValue = sanitizedValue.substring(0, MAX_TAG_VALUE_LENGTH);
        }

        return sanitizedValue;
    }

    public static String sanitizeNumber(Object numberValue) {
        if (numberValue == null) {
            return DEFAULT_TAG_VALUE;
        }
        String stringValue = String.valueOf(numberValue);
        // Простая проверка, что это число (возможно, с минусом или десятичной точкой)
        if (stringValue.matches("-?\\d+(\\.\\d+)?")) {
            return stringValue;
        }
        return sanitize(stringValue);
    }

    public static String sanitizeBoolean(Boolean boolValue) {
        if (boolValue == null) {
            return DEFAULT_TAG_VALUE;
        }
        return boolValue.toString();
    }
}
