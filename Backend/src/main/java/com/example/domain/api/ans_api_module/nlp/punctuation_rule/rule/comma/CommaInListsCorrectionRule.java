package com.example.domain.api.ans_api_module.nlp.punctuation_rule.rule.comma;

import com.example.domain.api.ans_api_module.nlp.punctuation_rule.correction.TextAnalysis;
import com.example.domain.api.ans_api_module.nlp.punctuation_rule.rule.CorrectionRule;

import java.util.Set;
import java.util.regex.Matcher;

public class CommaInListsCorrectionRule extends CorrectionRule {
    private static final Set<String> CONJUNCTIONS = Set.of(
            "и", "или", "а также", "либо", "ни"
    );

    private static final String LIST_REGEX =
            STR."(\\b\\p{L}+)(\\s+)(?=(\{String.join("|", CONJUNCTIONS)})\\b)";

    public CommaInListsCorrectionRule() {
        super(LIST_REGEX, true);
    }

    @Override
    public String apply(String text, TextAnalysis analysis) {
        if (!supportsLanguage(analysis.getLanguage())) {
            return text;
        }

        Matcher matcher = pattern.matcher(text);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            String word = matcher.group(1);
            if (!word.matches(".*[.,!?;:]$")) {
                matcher.appendReplacement(result, STR."\{word},");
            } else {
                matcher.appendReplacement(result, word);
            }
        }
        matcher.appendTail(result);

        return result.toString();
    }

    @Override
    public boolean supportsLanguage(String languageCode) {
        return "ru".equalsIgnoreCase(languageCode) ||
                "uk".equalsIgnoreCase(languageCode);
    }
}
