package com.example.domain.api.ans_api_module.answer_finder.search.score_combiner;

import com.example.domain.api.ans_api_module.answer_finder.domain.TrustScore;

public interface ScoreCombiner {

    /**
     * Комбинирует оценку сходства и TrustScore в единую итоговую оценку.
     *
     * @param similarityScore Оценка сходства
     * @param trustScore      TrustScore предопределенного ответа
     * @return Комбинированная итоговая оценка.
     */
    double combine(double similarityScore, TrustScore trustScore);
}
