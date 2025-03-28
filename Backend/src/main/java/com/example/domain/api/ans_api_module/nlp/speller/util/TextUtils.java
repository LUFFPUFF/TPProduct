package com.example.domain.api.ans_api_module.nlp.speller.util;

import com.example.domain.api.ans_api_module.nlp.punctuation_rule.correction_models.TextSegment;
import org.springframework.stereotype.Component;

import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

@Component
public class TextUtils {

    private static final Pattern WORD_PATTERN = Pattern.compile("\\p{L}+(?:['-]\\p{L}+)*");
    private static final Map<String, Locale> LANGUAGE_LOCALES = Map.of(
            "ru", new Locale("ru"),
            "en", Locale.ENGLISH
    );

    public List<TextSegment> splitIntoSegments(String text, String lang) {
        List<TextSegment> segments = new ArrayList<>();
        Locale locale = LANGUAGE_LOCALES.getOrDefault(lang, Locale.getDefault());
        BreakIterator iterator = BreakIterator.getSentenceInstance(locale);
        iterator.setText(text);

        int start = iterator.first();
        for (int end = iterator.next(); end != BreakIterator.DONE; start = end, end = iterator.next()) {
            segments.add(new TextSegment(start, end, text.substring(start, end).trim()));
        }
        return segments;
    }

    public int contextualDistance(String suggestion, String context, int position) {
        String contextWindow = context.substring(
                Math.max(0, position - 3),
                Math.min(context.length(), position + 3)
        );
        return optimizedLevenshtein(suggestion, contextWindow);
    }

    private int optimizedLevenshtein(String a, String b) {
        if (a.isEmpty()) return b.length();
        if (b.isEmpty()) return a.length();

        int[] prev = new int[b.length() + 1];
        for (int i = 0; i < prev.length; i++) prev[i] = i;

        for (int i = 0; i < a.length(); i++) {
            int[] curr = new int[b.length() + 1];
            curr[0] = i + 1;

            for (int j = 0; j < b.length(); j++) {
                int cost = (a.charAt(i) == b.charAt(j)) ? 0 : 1;
                curr[j+1] = Math.min(
                        Math.min(prev[j+1] + 1, curr[j] + 1),
                        prev[j] + cost
                );
            }
            prev = curr;
        }
        return prev[b.length()];
    }
}
