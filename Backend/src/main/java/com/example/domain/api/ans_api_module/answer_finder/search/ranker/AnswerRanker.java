package com.example.domain.api.ans_api_module.answer_finder.search.ranker;

import com.example.domain.api.ans_api_module.answer_finder.search.dto.ScoredAnswerCandidate;

import java.util.List;

public interface AnswerRanker {

    /**
     * Ранжирует (сортирует) список кандидатов ответа.
     *
     * @param candidates Список кандидатов ответа с вычисленными оценками ({@link ScoredAnswerCandidate}).
     * @return Отсортированный список кандидатов ответа, где первые элементы являются наиболее релевантными.
     */
    List<ScoredAnswerCandidate> rank(List<ScoredAnswerCandidate> candidates);
}
