package com.example.domain.api.ans_api_module.nlp.punctuation_rule.rule.dot;

import com.example.domain.api.ans_api_module.nlp.punctuation_rule.correction.TextAnalysis;
import com.example.domain.api.ans_api_module.nlp.punctuation_rule.rule.CorrectionRule;

import java.util.Set;
import java.util.regex.Pattern;

public class SentenceEndingDotCorrectionRule extends CorrectionRule {

    private static final Set<String> ABBREVIATIONS = Set.of(
            "т.д.", "т.п.", "и.о.", "проф.", "стр.", "др.", "см.", "рис.", "гл."
    );

    private static final Pattern SENTENCE_END_PATTERN = Pattern.compile(
            "([а-яА-ЯёЁa-zA-Z0-9])(\\s*)$",
            Pattern.UNICODE_CHARACTER_CLASS
    );

    public SentenceEndingDotCorrectionRule() {
        super("", true);
    }

    @Override
    public String apply(String text, TextAnalysis analysis) {
        if (!supportsLanguage(analysis.getLanguage())) {
            return text;
        }

        String[] sentences = text.split("(?<=[.!?])\\s+");
        StringBuilder result = new StringBuilder();

        for (String sentence : sentences) {
            String trimmed = sentence.trim();
            if (trimmed.isEmpty()) continue;

            char lastChar = trimmed.charAt(trimmed.length() - 1);
            if (lastChar == '?' || lastChar == '!') {
                result.append(trimmed).append(" ");
                continue;
            }

            if (shouldAddDot(trimmed)) {
                trimmed = addProperDot(trimmed);
            }

            result.append(trimmed).append(" ");
        }

        return result.toString().trim();
    }

    private boolean shouldAddDot(String sentence) {
        return !sentence.endsWith(".") &&
                !isAbbreviation(sentence) &&
                Character.isLetter(sentence.charAt(sentence.length() - 1));
    }

    private boolean isAbbreviation(String text) {
        String lower = text.toLowerCase();
        return ABBREVIATIONS.stream().anyMatch(lower::contains) ||
                text.matches(".*\\b([а-яА-ЯёЁ])\\.$");
    }

    private String addProperDot(String sentence) {
        return STR."\{sentence.replaceAll("\\s+$", "")}.";
    }

    private String normalizeSentenceEnding(String sentence) {
        while (sentence.endsWith(".")) {
            sentence = sentence.substring(0, sentence.length() - 1).trim();
        }
        return sentence.isEmpty() ? "" : STR."\{sentence}.";
    }

}
