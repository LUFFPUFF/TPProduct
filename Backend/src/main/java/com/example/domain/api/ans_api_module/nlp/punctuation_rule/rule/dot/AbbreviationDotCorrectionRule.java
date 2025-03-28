package com.example.domain.api.ans_api_module.nlp.punctuation_rule.rule.dot;

import com.example.domain.api.ans_api_module.nlp.punctuation_rule.correction.TextAnalysis;
import com.example.domain.api.ans_api_module.nlp.punctuation_rule.rule.CorrectionRule;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AbbreviationDotCorrectionRule extends CorrectionRule {

    private static final Map<String, String> ABBREVIATION_MAP = Map.of(
            "тд", "т.д.",
            "тп", "т.п.",
            "ио", "и.о.",
            "проф", "проф.",
            "стр", "стр.",
            "др", "др.",
            "см", "см.",
            "рис", "рис.",
            "гл", "гл."
    );

    private static final String ABBREVIATION_REGEX =
            STR."\\b(\{String.join("|", ABBREVIATION_MAP.keySet())})\\b";

    public AbbreviationDotCorrectionRule() {
        super(ABBREVIATION_REGEX, true);
    }

    @Override
    public String apply(String text, TextAnalysis analysis) {
        Matcher matcher = pattern.matcher(text);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String found = matcher.group(1).toLowerCase();
            String replacement = ABBREVIATION_MAP.get(found);
            if (replacement != null && !isInNumericContext(text, matcher.start())) {
                matcher.appendReplacement(result, replacement);
            }
        }
        matcher.appendTail(result);

        return result.toString();
    }

    private boolean isInNumericContext(String text, int pos) {
        return pos > 0 && Character.isDigit(text.charAt(pos - 1));
    }

    @Override
    public boolean supportsLanguage(String languageCode) {
        return "ru".equalsIgnoreCase(languageCode);
    }
}
