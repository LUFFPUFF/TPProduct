package com.example.domain.api.ans_api_module.answer_finder.search;

import com.example.database.model.ai_module.PredefinedAnswer;
import com.example.domain.api.ans_api_module.answer_finder.domain.TrustScore;
import com.example.domain.api.ans_api_module.answer_finder.exception.AnswerSearchException;
import com.example.domain.api.ans_api_module.answer_finder.exception.EmbeddingException;
import com.example.domain.api.ans_api_module.answer_finder.nlp.hf_similarity_client.HuggingFaceSentenceSimilarityClient;
import com.example.domain.api.ans_api_module.answer_finder.search.dto.ScoredAnswerCandidate;
import com.example.domain.api.ans_api_module.answer_finder.search.ranker.AnswerRanker;
import com.example.domain.api.ans_api_module.answer_finder.search.score_combiner.ScoreCombiner;
import com.example.domain.api.ans_api_module.answer_finder.search.similarity_calculator.EmbeddingSimilarityCalculator;
import com.example.domain.api.ans_api_module.answer_finder.search.similarity_calculator.SimilarityCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class AnswerMatcher {

    private final SimilarityCalculator similarityCalculator;
    private final ScoreCombiner combiner;
    private final AnswerRanker ranker;

    /**
     * Находит и ранжирует наиболее релевантные предопределенные ответы
     * из предоставленного списка потенциальных кандидатов на основе запроса клиента.
     *
     * @param clientQuery     Текст запроса клиента.
     * @param potentialAnswers Список предопределенных ответов, потенциально подходящих.
     * @return Отсортированный список {@link ScoredAnswerCandidate}.
     *         Возвращает пустой список, если входные данные некорректны или процесс не дал результатов.
     * @throws AnswerSearchException В случае ошибки во время процесса поиска.
     */
    public List<ScoredAnswerCandidate> findBestAnswers(
            String clientQuery,
            List<PredefinedAnswer> potentialAnswers) throws AnswerSearchException {

        //TODO продумать что будет если не найдется лучшего варианта

        if (clientQuery == null || clientQuery.trim().isEmpty()) {
            log.warn("Получен пустой или null запрос клиента. Невозможно выполнить поиск.");
            return Collections.emptyList();
        }
        if (potentialAnswers == null || potentialAnswers.isEmpty()) {
            log.debug("Получен пустой или null список потенциальных ответов.");
            return Collections.emptyList();
        }

        try {
            Map<Integer, Double> similarityScores = calculateSimilarityScores(clientQuery, potentialAnswers);
            if (similarityScores.isEmpty()) {
                log.warn("Расчет оценок сходства не вернул результатов. Возвращаем пустой список.");
                return Collections.emptyList();
            }

            List<ScoredAnswerCandidate> scoredCandidates = combineScoresAndCreateCandidates(potentialAnswers, similarityScores);
            if (scoredCandidates.isEmpty()) {
                log.warn("Комбинирование оценок не дало валидных кандидатов. Возвращаем пустой список.");
                return Collections.emptyList();
            }

            return rankCandidates(scoredCandidates);
        } catch (Exception e) {
            log.error("Произошла ошибка во время процесса поиска ответов для запроса: '{}'.",
                    clientQuery.substring(0, Math.min(clientQuery.length(), 100)) + "...", e);

            if (e instanceof AnswerSearchException) {
                throw (AnswerSearchException) e;
            } else {
                throw new AnswerSearchException("Ошибка во время поиска ответов для запроса: " + clientQuery, e);
            }
        }
    }

    private Map<Integer, Double> calculateSimilarityScores(String clientQuery, List<PredefinedAnswer> potentialAnswers) throws AnswerSearchException {
        try {
            return similarityCalculator.calculateSimilarities(clientQuery, potentialAnswers);
        } catch (EmbeddingException e) {
            log.error("Ошибка EmbeddingException при вызове SimilarityCalculator.", e);
            throw new AnswerSearchException("Ошибка при расчете оценок сходства.", e);
        } catch (Exception e) {
            log.error("Неожиданная ошибка при вызове SimilarityCalculator.", e);
            throw new AnswerSearchException("Неожиданная ошибка при расчете оценок сходства.", e);
        }
    }

    private List<ScoredAnswerCandidate> combineScoresAndCreateCandidates(
            List<PredefinedAnswer> potentialAnswers,
            Map<Integer, Double> similarityScores
    ) {
        List<ScoredAnswerCandidate> scoredCandidates  = new ArrayList<>();

        for (PredefinedAnswer answer : potentialAnswers) {
            if (answer == null || answer.getId() == null) {
                log.warn("Пропущен некорректный (null или без ID) предопределенный ответ в списке потенциальных.");
                continue;
            }
            if (answer.getTrustScore() == null) {
                log.warn("Пропущен предопределенный ответ ID {} с TrustScore=null.", answer.getId());
                continue;
            }

            Double similarityScore = similarityScores.getOrDefault(answer.getId(), 0.0);
            if (similarityScore == 0.0 && !similarityScores.containsKey(answer.getId())) {
                log.warn("Оценка сходства отсутствует в Map для ответа с ID {}. Использована оценка 0.0.", answer.getId());
            }

            TrustScore trustScore = answer.getTrustScore();
            double combinedScore = combiner.combine(similarityScore, trustScore);

            ScoredAnswerCandidate scoredCandidate = ScoredAnswerCandidate.builder()
                    .originalAnswer(answer)
                    .similarityScore(similarityScore)
                    .combinedScore(combinedScore)
                    .trustScore(trustScore)
                    .build();

            scoredCandidates.add(scoredCandidate);

        }
        log.debug("Создано {} ScoredAnswerCandidate.", scoredCandidates.size());
        return scoredCandidates;
    }

    private List<ScoredAnswerCandidate> rankCandidates(List<ScoredAnswerCandidate> scoredCandidates) {
        return ranker.rank(scoredCandidates);
    }
}
