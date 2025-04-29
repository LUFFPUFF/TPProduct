package com.example.domain.api.ans_api_module.answer_finder.domain;

import com.example.database.model.ai_module.PredefinedAnswer;
import com.example.domain.api.ans_api_module.answer_finder.domain.dto.PredefinedAnswerDto;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class AnswerCandidate {

    private PredefinedAnswer predefinedAnswer;

    private double similarityScore;
    private TrustScore trustScore;
    private double combinedScore;

    public Integer getId() {
        return predefinedAnswer != null ? predefinedAnswer.getId() : null;
    }

    public String getAnswerText() {
        return predefinedAnswer != null ? predefinedAnswer.getAnswer() : null;
    }

    public TrustScore getTrustScoreObject() {
        return this.trustScore;
    }

    @Override
    public String toString() {
        return "AnswerCandidate{" +
                "answerId=" + getId() +
                ", similarityScore=" + similarityScore +
                ", combinedScore=" + combinedScore +
                ", trustScore=" + (trustScore != null ? trustScore.getScore() : "null") +
                ", answerTextSnippet='" + (getAnswerText() != null ? getAnswerText().substring(0, Math.min(getAnswerText().length(), 50)) + "..." : "null") + '\'' +
                '}';
    }
}
