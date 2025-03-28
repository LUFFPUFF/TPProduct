package com.example.domain.api.ans_api_module.nlp.punctuation_rule.correction_models;

import java.util.Collections;
import java.util.List;

public record CorrectionResult(String correctedText, List<Correction> corrections) {
    public static CorrectionResult empty(String text) {
        return new CorrectionResult(text, Collections.emptyList());
    }
}
