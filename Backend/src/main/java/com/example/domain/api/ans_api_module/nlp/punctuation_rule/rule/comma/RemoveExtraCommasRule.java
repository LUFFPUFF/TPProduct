package com.example.domain.api.ans_api_module.nlp.punctuation_rule.rule.comma;

import com.example.domain.api.ans_api_module.nlp.punctuation_rule.correction.TextAnalysis;
import com.example.domain.api.ans_api_module.nlp.punctuation_rule.rule.CorrectionRule;

import java.util.regex.Matcher;

public class RemoveExtraCommasRule extends CorrectionRule {
    private static final String EXTRA_COMMAS_REGEX =
            ",{2,}|,(?=\\s*[.,!?;:])";

    public RemoveExtraCommasRule() {
        super(EXTRA_COMMAS_REGEX, true);
    }

    @Override
    public String apply(String text, TextAnalysis analysis) {
        Matcher matcher = pattern.matcher(text);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            if (matcher.group().startsWith(",")) {
                matcher.appendReplacement(result, "");
            }
        }
        matcher.appendTail(result);

        return result.toString().replaceAll(",(?=\\s*[.!?])", "");
    }

}
