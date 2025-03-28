package com.example.domain.api.ans_api_module.nlp.punctuation_rule.rule.question;

import com.example.domain.api.ans_api_module.nlp.punctuation_rule.correction.TextAnalysis;
import com.example.domain.api.ans_api_module.nlp.punctuation_rule.rule.CorrectionRule;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class QuestionMarkCorrectionRule extends CorrectionRule {

    private static final Set<String> QUESTION_WORDS = Set.of(
            "кто", "что", "какой", "какая", "какое", "какие", "чей", "чья", "чьё", "чьи",
            "который", "которая", "которое", "которые",

            "где", "куда", "откуда", "когда", "почему", "отчего", "зачем", "как",
            "насколько", "сколько", "каким образом", "с каких пор", "до каких пор",

            "как жизнь", "как настроение", "как успехи", "как работа",

            "ли", "разве", "неужели", "уж не", "а что если",

            "в чем дело", "что случилось", "как дела", "что нового", "как поживаешь",
            "в чем причина", "что произошло", "как ты", "как вы", "что ты", "что вы",

            "можно ли", "стоит ли", "правильно ли", "действительно ли", "возможно ли",
            "нужно ли", "обязательно ли", "следует ли", "должен ли",

            "или", "либо",

            "как насчет", "что насчет", "что думаешь", "как считаешь", "каково",
            "каков", "какова", "каковы", "в чем смысл", "в чем суть", "в чем разница",

            "а", "да", "нет", "так", "ну", "ведь", "же",

            "есть ли", "имеется ли", "существует ли", "будет ли", "стал ли",
            "оказался ли", "явился ли", "сможет ли", "сможете ли", "получится ли",

            "который час", "сколько времени", "какого числа", "какого года",

            "знаешь ли", "помнишь ли", "понимаешь ли", "слышал ли", "видел ли",

            "не", "ни", "разве не", "неужели не", "или нет", "или не",

            "чем", "как будто", "словно", "точно"
    );

    private static final Pattern QUESTION_PATTERN = Pattern.compile(
            "([?]+)|([.!]$)|(\\s+[?]+)|(\\?\\s*$)",
            Pattern.UNICODE_CHARACTER_CLASS
    );

    public QuestionMarkCorrectionRule() {
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

            trimmed = trimmed.replaceAll("\\?+", "?");

            boolean isQuestion = isQuestion(trimmed, analysis);

            char lastChar = trimmed.charAt(trimmed.length() - 1);
            if (isQuestion && lastChar != '?') {
                if (".!".indexOf(lastChar) >= 0) {
                    trimmed = trimmed.substring(0, trimmed.length() - 1) + "?";
                } else {
                    trimmed += "?";
                }
            } else if (!isQuestion && lastChar == '?') {
                trimmed = trimmed.substring(0, trimmed.length() - 1) + ".";
            }

            result.append(trimmed).append(" ");
        }

        return result.toString().trim();
    }

    private boolean isQuestion(String sentence, TextAnalysis analysis) {
        String lower = sentence.toLowerCase();

        if (QUESTION_WORDS.stream().anyMatch(word ->
                lower.startsWith(word + " ") ||
                        lower.contains(" " + word + " ") ||
                        lower.endsWith(" " + word))) {
            return true;
        }

        if (analysis.getLanguage().equals("ru")) {
            String[] words = sentence.split("\\s+");
            if (words.length > 1 && isVerbFirst(words)) {
                return true;
            }
        }

        return analysis.isQuestionSentence(sentence);
    }

    private boolean isVerbFirst(String[] words) {
        String firstWord = words[0].toLowerCase();
        return firstWord.endsWith("ть") ||
                firstWord.endsWith("ться") ||
                firstWord.matches("(буду|будет|был|была|было|были)");
    }

}
