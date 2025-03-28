package com.example.domain.api.ans_api_module.nlp.punctuation_rule.correction_models;

import java.util.ArrayList;
import java.util.List;

public class TextDiff {

    public static List<Correction> compare(String original, String corrected, CorrectionType type) {
        List<Correction> corrections = new ArrayList<>();
        if (!original.equals(corrected)) {
            corrections.add(new Correction(
                    0,
                    original,
                    corrected,
                    type
            ));
        }
        return corrections;
    }

    public static int contextualDistance(String suggestion, String context, int position) {
        if (context.isEmpty() || position < 0 || position >= context.length()) {
            return Integer.MAX_VALUE;
        }

        int windowStart = Math.max(0, position - 15);
        int windowEnd = Math.min(context.length(), position + 15);
        String contextWindow = context.substring(windowStart, windowEnd);

        return levenshteinDistance(suggestion.toLowerCase(), contextWindow.toLowerCase());
    }

    private static int levenshteinDistance(String a, String b) {
        int[][] dp = new int[a.length() + 1][b.length() + 1];

        for (int i = 0; i <= a.length(); i++) {
            for (int j = 0; j <= b.length(); j++) {
                if (i == 0) {
                    dp[i][j] = j;
                } else if (j == 0) {
                    dp[i][j] = i;
                } else {
                    dp[i][j] = min(
                            dp[i - 1][j - 1] + (a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1),
                            dp[i - 1][j] + 1,
                            dp[i][j - 1] + 1
                    );
                }
            }
        }
        return dp[a.length()][b.length()];
    }

    private static int min(int a, int b, int c) {
        return Math.min(a, Math.min(b, c));
    }
}
