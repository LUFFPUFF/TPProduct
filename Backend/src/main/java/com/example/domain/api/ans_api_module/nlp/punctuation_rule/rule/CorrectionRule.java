package com.example.domain.api.ans_api_module.nlp.punctuation_rule.rule;

import com.example.domain.api.ans_api_module.nlp.punctuation_rule.correction.TextAnalysis;
import lombok.Getter;

import java.util.regex.Pattern;

public abstract class CorrectionRule {
    @Getter
    protected final String ruleName;
    protected final Pattern pattern;
    @Getter
    protected final boolean enabledByDefault;

    protected CorrectionRule(String regex, boolean enabledByDefault) {
        this.ruleName = this.getClass().getSimpleName();
        this.pattern = Pattern.compile(regex, Pattern.UNICODE_CHARACTER_CLASS);
        this.enabledByDefault = enabledByDefault;
    }

    public abstract String apply(String text, TextAnalysis analysis);

    protected String replacePreservingCase(String original, String replacement) {
        if (original.isEmpty()) return replacement;

        char firstChar = original.charAt(0);
        if (Character.isUpperCase(firstChar)) {
            return replacement.substring(0, 1).toUpperCase() +
                    (replacement.length() > 1 ? replacement.substring(1) : "");
        }
        return replacement;
    }

    public boolean supportsLanguage(String languageCode) {
        return true;
    }

}
