package com.example.domain.api.ans_api_module.answer_finder.search.similarity_calculator;

import com.example.database.model.ai_module.PredefinedAnswer;
import com.example.domain.api.ans_api_module.answer_finder.exception.EmbeddingException;

import java.util.List;
import java.util.Map;

public interface SimilarityCalculator {

    /**
     * Вычисляет оценку сходства между запросом клиента и списком предопределенных ответов.
     *
     * @param clientQuery     Текст запроса клиента.
     * @param potentialAnswers Список предопределенных ответов (доменных сущностей {@link PredefinedAnswer}) для сравнения.
     * @return Map, где ключ - ID предопределенного ответа ({@link PredefinedAnswer#getId()}),
     *         значение - вычисленная оценка сходства (тип Double).
     * @throws EmbeddingException Если произошла ошибка при взаимодействии с внешним API
     *                            (например, через SentenceSimilarityClient).
     */
    Map<Integer, Double> calculateSimilarities(String clientQuery, List<PredefinedAnswer> potentialAnswers) throws EmbeddingException;
}
