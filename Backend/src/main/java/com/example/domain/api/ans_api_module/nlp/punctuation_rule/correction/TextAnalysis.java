package com.example.domain.api.ans_api_module.nlp.punctuation_rule.correction;

import com.example.domain.api.ans_api_module.nlp.punctuation_rule.correction_models.RuleChange;
import com.example.domain.api.ans_api_module.nlp.punctuation_rule.correction_models.TextSegment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class TextAnalysis {
    private final String lang;
    private final Map<String, Integer> wordFreq;
    private final List<RuleChange> changes = new ArrayList<>();

    private static final Set<String> QUESTION_WORDS = Set.of(
            "ли", "разве", "неужели", "кто", "что", "где", "когда",
            "куда", "откуда", "почему", "зачем", "как", "чей", "кого", "кому"
    );

    private static final Set<String> EXCLAMATION_WORDS = Set.of(
            "какой", "какая", "какое", "какие", "сколько", "так", "вот",
            "ну", "ах", "ох", "ого", "ух", "вау", "ура", "боже", "черт", "круто"
    );

    private static final Set<String> EMOTIONAL_PUNCTUATION = Set.of("!", "!!", "!!!", "!?");
    private static final Set<String> QUESTION_PUNCTUATION = Set.of("?", "??", "???", "?!");

    public TextAnalysis(@Autowired(required = false) String lang,
                        @Autowired(required = false) Map<String, Integer> wordFreq,
                        Set<String> strings) {
        this.lang = lang;
        this.wordFreq = wordFreq;
    }

    public List<TextSegment> segment(String text) {
        List<TextSegment> segments = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return segments;
        }

        int start = 0;
        int end = 0;
        while (end < text.length()) {
            char c = text.charAt(end);
            if (isSentenceEnd(c)) {
                String sentence = text.substring(start, end + 1).trim();
                if (!sentence.isEmpty()) {
                    segments.add(new TextSegment(start, end + 1, sentence));
                }
                start = end + 1;
            }
            end++;
        }

        if (start < text.length()) {
            String remaining = text.substring(start).trim();
            if (!remaining.isEmpty()) {
                segments.add(new TextSegment(start, text.length(), remaining));
            }
        }

        return segments;
    }

    private boolean isSentenceEnd(char c) {
        return c == '.' || c == '!' || c == '?';
    }

    public TextAnalysis analyze(String text, String lang) {
        return new TextAnalysis(
                lang,
                calculateWordFrequencies(text),
                extractAbbreviations(text)
        );
    }

    private Map<String, Integer> calculateWordFrequencies(String text) {
        return Arrays.stream(text.split("\\s+"))
                .filter(word -> !word.isEmpty())
                .collect(Collectors.toMap(
                        word -> word,
                        _ -> 1,
                        Integer::sum
                ));
    }

    private Set<String> extractAbbreviations(String text) {
        return Arrays.stream(text.split("\\s+"))
                .filter(this::isAbbreviation)
                .collect(Collectors.toSet());
    }

    private boolean isAbbreviation(String word) {
        if (word.isEmpty()) return false;

        return word.chars()
                .allMatch(c -> Character.isUpperCase(c) || Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CYRILLIC);
    }

    public void recordChange(Class<?> ruleClass, String before, String after) {
        changes.add(new RuleChange(ruleClass, before, after));
    }

    public int getWordFrequency(String word) {
        return wordFreq.getOrDefault(word, 0);
    }

    public String getLanguage() {
        return lang;
    }

    public boolean isEmotionalSentence(String sentence) {
        if (sentence == null || sentence.isEmpty()) return false;

        String lastChar = sentence.substring(sentence.length() - 1);
        if (EMOTIONAL_PUNCTUATION.contains(lastChar)) {
            return true;
        }

        return containsAnyWordIgnoreCase(sentence, EXCLAMATION_WORDS);
    }

    public boolean isQuestionSentence(String sentence) {
        if (sentence == null || sentence.isEmpty()) return false;

        char lastChar = sentence.charAt(sentence.length() - 1);
        if (lastChar == '?') return true;

        if (containsAnyWordIgnoreCase(sentence, QUESTION_WORDS)) {
            return true;
        }

        return "ru".equalsIgnoreCase(lang) && hasQuestionWordOrder(sentence);
    }

    private boolean containsAnyWordIgnoreCase(String text, Set<String> words) {
        String lowerText = text.toLowerCase();
        return words.stream().anyMatch(word ->
                lowerText.contains(" " + word + " ") ||
                        lowerText.startsWith(word + " ") ||
                        lowerText.endsWith(" " + word)
        );
    }

    private boolean hasQuestionWordOrder(String sentence) {
        String[] words = sentence.split("\\s+");
        if (words.length < 2) return false;

        boolean verbBeforeSubject = false;
        boolean hasSubject = false;

        for (String word : words) {
            if (isVerb(word)) {
                if (hasSubject) {
                    verbBeforeSubject = true;
                    break;
                }
            } else if (isNoun(word)) {
                hasSubject = true;
            }
        }

        return verbBeforeSubject;
    }

    private boolean isVerb(String word) {
        return word.endsWith("ть") || word.endsWith("ти");
    }

    private boolean isNoun(String word) {
        return word.length() > 3 && Character.isUpperCase(word.charAt(0));
    }

    public List<RuleChange> getChanges() {
        return Collections.unmodifiableList(changes);
    }
}
