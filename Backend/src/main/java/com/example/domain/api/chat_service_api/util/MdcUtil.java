package com.example.domain.api.chat_service_api.util;

import org.slf4j.MDC;

import java.util.HashMap;
import java.util.Map;

public final class MdcUtil {

    private MdcUtil() {}

    public static Map<String, String> createContextMap(Object... keyValuePairs) {
        if (keyValuePairs == null || keyValuePairs.length == 0) {
            return new HashMap<>();
        }
        if (keyValuePairs.length % 2 != 0) {
            throw new IllegalArgumentException("Must provide an even number of arguments for key-value pairs.");
        }
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < keyValuePairs.length; i += 2) {
            String key = String.valueOf(keyValuePairs[i]);
            String value = keyValuePairs[i + 1] != null ? String.valueOf(keyValuePairs[i + 1]) : "null";
            map.put(key, value);
        }
        return map;
    }

    public static <T, E extends Throwable> T withContext(Map<String, String> context, ThrowingSupplier<T, E> supplier) throws E {
        if (context == null || context.isEmpty()) {
            return supplier.get();
        }

        Map<String, String> previousMdcValues = new HashMap<>();
        for (Map.Entry<String, String> entry : context.entrySet()) {
            previousMdcValues.put(entry.getKey(), MDC.get(entry.getKey()));
            MDC.put(entry.getKey(), entry.getValue());
        }

        try {
            return supplier.get();
        } finally {
            for (Map.Entry<String, String> entry : context.entrySet()) {
                String previousValue = previousMdcValues.get(entry.getKey());
                if (previousValue != null) {
                    MDC.put(entry.getKey(), previousValue);
                } else {
                    MDC.remove(entry.getKey());
                }
            }
        }
    }

    public static <T, E extends Throwable> T withContext(ThrowingSupplier<T, E> supplier, Object... keyValuePairs) throws E {
        Map<String, String> contextMap = createContextMap(keyValuePairs);
        return withContext(contextMap, supplier);
    }

    @FunctionalInterface
    public interface ThrowingSupplier<T, E extends Throwable> {
        T get() throws E;
    }
}
