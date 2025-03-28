package com.example.domain.api.ans_api_module.nlp.punctuation_rule.rule.comma;

import com.example.domain.api.ans_api_module.nlp.punctuation_rule.correction.TextAnalysis;
import com.example.domain.api.ans_api_module.nlp.punctuation_rule.rule.CorrectionRule;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class CommaBeforeConjunctionRule extends CorrectionRule {

    private static final Set<String> CONJUNCTIONS = Set.of(
            "но", "а", "да", "однако", "зато", "или", "либо", "то есть", "а именно"
    );

    private static final String CONJUNCTION_PATTERN =
            STR."(?<!,)\\s+(\{String.join("|", CONJUNCTIONS)})\\b";

    public CommaBeforeConjunctionRule() {
        super(CONJUNCTION_PATTERN, true);
    }

    @Override
    public String apply(String text, TextAnalysis analysis) {
        if (!supportsLanguage(analysis.getLanguage())) {
            return text;
        }

        Matcher matcher = pattern.matcher(text);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            String conjunction = matcher.group(1);
            String replacement = STR.", \{conjunction}";

            if (!isInNumericContext(text, matcher.start())) {
                matcher.appendReplacement(result, replacement);
            } else {
                matcher.appendReplacement(result, " " + conjunction);
            }
        }
        matcher.appendTail(result);

        return result.toString();
    }

    private boolean isInNumericContext(String text, int pos) {
        if (pos < 1 || pos >= text.length()) return false;

        return Character.isDigit(text.charAt(pos - 1));
    }

    @Override
    public boolean supportsLanguage(String languageCode) {
        return "ru".equalsIgnoreCase(languageCode) ||
                "uk".equalsIgnoreCase(languageCode);
    }
}
