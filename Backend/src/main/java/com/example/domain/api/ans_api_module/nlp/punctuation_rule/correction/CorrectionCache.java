package com.example.domain.api.ans_api_module.nlp.punctuation_rule.correction;

import com.example.domain.api.ans_api_module.nlp.punctuation_rule.correction_models.CorrectionResult;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class CorrectionCache {
    private final Map<String, CorrectionResult> cache = new ConcurrentHashMap<>();
    private final int MAX_SIZE = 10000;

    public CorrectionResult get(String key) {
        return cache.get(key);
    }

    public void put(String key, CorrectionResult result) {
        if (cache.size() >= MAX_SIZE) {
            cache.clear();
        }
        cache.put(key, result);
    }
}
