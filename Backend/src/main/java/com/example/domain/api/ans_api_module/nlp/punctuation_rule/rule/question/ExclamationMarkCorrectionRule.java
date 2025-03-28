package com.example.domain.api.ans_api_module.nlp.punctuation_rule.rule.question;

import com.example.domain.api.ans_api_module.nlp.punctuation_rule.correction.TextAnalysis;
import com.example.domain.api.ans_api_module.nlp.punctuation_rule.rule.CorrectionRule;

import java.util.Set;


public class ExclamationMarkCorrectionRule extends CorrectionRule {

    private static final Set<String> EXCLAMATION_WORDS = Set.of(
            "какой", "какая", "какое", "какие", "сколько",
            "так", "вот", "ну", "ах", "ох", "ого", "ух",
            "вау", "ура", "боже", "черт", "дьявол", "круто"
    );

    private static final String EXCLAMATION_REGEX =
            "([!?]{2,})|([.!?]$)";

    public ExclamationMarkCorrectionRule() {
        super(EXCLAMATION_REGEX, true);
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

            trimmed = trimmed.replaceAll("!+", "!");

            boolean isExclamation = isExclamation(trimmed, analysis);

            char lastChar = trimmed.charAt(trimmed.length() - 1);
            if (isExclamation && lastChar != '!') {
                if (".?".indexOf(lastChar) >= 0) {
                    trimmed = STR."\{trimmed.substring(0, trimmed.length() - 1)}!";
                } else {
                    trimmed += "!";
                }
            }

            result.append(trimmed).append(" ");
        }

        return result.toString().trim();
    }

    private boolean isExclamation(String sentence, TextAnalysis analysis) {
        String lower = sentence.toLowerCase();
        return EXCLAMATION_WORDS.stream().anyMatch(word ->
                lower.startsWith(STR."\{word} ") ||
                        lower.contains(STR." \{word} ") ||
                        lower.endsWith(STR." \{word}")) ||
                analysis.isEmotionalSentence(sentence);
    }

    private String ensureExclamationMark(String sentence) {
        char lastChar = sentence.charAt(sentence.length() - 1);
        if (lastChar != '!') {
            return STR."\{sentence.substring(0, sentence.length() - 1)}!";
        }
        return sentence;
    }

}
