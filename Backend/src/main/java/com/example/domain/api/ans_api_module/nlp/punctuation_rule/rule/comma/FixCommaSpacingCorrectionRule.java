package com.example.domain.api.ans_api_module.nlp.punctuation_rule.rule.comma;

import com.example.domain.api.ans_api_module.nlp.punctuation_rule.correction.TextAnalysis;
import com.example.domain.api.ans_api_module.nlp.punctuation_rule.rule.CorrectionRule;

import java.util.regex.Matcher;

public class FixCommaSpacingCorrectionRule extends CorrectionRule {
    private static final String COMMA_SPACING_REGEX =
            "\\s*,\\s*";

    public FixCommaSpacingCorrectionRule() {
        super(COMMA_SPACING_REGEX, true);
    }

    @Override
    public String apply(String text, TextAnalysis analysis) {
        Matcher matcher = pattern.matcher(text);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            if (!isInNumericContext(text, matcher.start())) {
                matcher.appendReplacement(result, ", ");
            } else {
                matcher.appendReplacement(result, ",");
            }
        }
        matcher.appendTail(result);

        return result.toString();
    }

    private boolean isInNumericContext(String text, int pos) {
        if (pos <= 0 || pos >= text.length()) return false;

        return Character.isDigit(text.charAt(pos - 1)) &&
                pos + 1 < text.length() &&
                Character.isDigit(text.charAt(pos + 1));
    }

}
