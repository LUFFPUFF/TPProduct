package com.example.domain.api.ans_api_module.answer_finder.search.score_combiner;

import com.example.domain.api.ans_api_module.answer_finder.domain.TrustScore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class WeightedSumScoreCombiner implements ScoreCombiner {

    private final double weightSimilarity;
    private final double weightTrust;

    @Override
    public double combine(double similarityScore, TrustScore trustScore) {

        double trustValue = (trustScore != null) ? trustScore.getScore() : 0.0;

        if (trustScore  == null) {
            log.warn("TrustScore объект null для комбинирования. Использовано значение 0.0.");
        }

        //TODO логика очень простая, но пока так

        return (weightSimilarity * similarityScore) + (weightTrust * trustValue);
    }
}
