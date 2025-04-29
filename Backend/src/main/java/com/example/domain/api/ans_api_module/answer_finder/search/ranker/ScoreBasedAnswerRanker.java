package com.example.domain.api.ans_api_module.answer_finder.search.ranker;

import com.example.domain.api.ans_api_module.answer_finder.search.dto.ScoredAnswerCandidate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Slf4j
public class ScoreBasedAnswerRanker implements AnswerRanker {

    @Override
    public List<ScoredAnswerCandidate> rank(List<ScoredAnswerCandidate> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            log.debug("Список кандидатов пуст или null. Ранжирование не требуется.");
            return new ArrayList<>();
        }

        List<ScoredAnswerCandidate> mutableCandidates = new ArrayList<>(candidates);

        Comparator<ScoredAnswerCandidate> byCombinedScoreDescending =
                Comparator.comparingDouble(ScoredAnswerCandidate::getCombinedScore).reversed();

        mutableCandidates.sort(byCombinedScoreDescending);

        return mutableCandidates;
    }
}
