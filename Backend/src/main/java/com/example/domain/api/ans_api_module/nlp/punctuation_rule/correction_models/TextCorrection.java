package com.example.domain.api.ans_api_module.nlp.punctuation_rule.correction_models;

public record TextCorrection(int start, int end, String original,
                             String corrected, String ruleName) {
    public TextCorrection withAdjustedPosition(int offset) {
        return new TextCorrection(start + offset, end + offset,
                original, corrected, ruleName);
    }
}
