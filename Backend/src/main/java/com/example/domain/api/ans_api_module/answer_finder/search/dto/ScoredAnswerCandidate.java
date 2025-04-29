package com.example.domain.api.ans_api_module.answer_finder.search.dto;

import com.example.database.model.ai_module.PredefinedAnswer;
import com.example.domain.api.ans_api_module.answer_finder.domain.TrustScore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScoredAnswerCandidate {

    private PredefinedAnswer originalAnswer;
    private double similarityScore;
    private TrustScore trustScore;
    private double combinedScore;

    public Integer getId() {
        return originalAnswer != null ? originalAnswer.getId() : null;
    }

    public String getAnswerText() {
        return originalAnswer != null ? originalAnswer.getAnswer() : null;
    }

    public String getTitle() {
        return originalAnswer != null ? originalAnswer.getTitle() : null;
    }

    @Override
    public String toString() {
        return "ScoredAnswerCandidate{" +
                "answerId=" + getId() +
                ", similarityScore=" + similarityScore +
                ", trustScore=" + (trustScore != null ? trustScore.getScore() : "null") + // Выводим значение TrustScore
                ", combinedScore=" + combinedScore +
                ", answerTextSnippet='" + (getAnswerText() != null ? getAnswerText().substring(0, Math.min(getAnswerText().length(), 50)) + "..." : "null") + '\'' +
                '}';
    }
}
