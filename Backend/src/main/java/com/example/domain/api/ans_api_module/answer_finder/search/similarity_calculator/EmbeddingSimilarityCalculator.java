package com.example.domain.api.ans_api_module.answer_finder.search.similarity_calculator;

import com.example.database.model.ai_module.PredefinedAnswer;
import com.example.domain.api.ans_api_module.answer_finder.exception.EmbeddingException;
import com.example.domain.api.ans_api_module.answer_finder.nlp.hf_similarity_client.SentenceSimilarityClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Slf4j
public class EmbeddingSimilarityCalculator implements SimilarityCalculator {

    private final SentenceSimilarityClient sentenceSimilarityClient;

    @Override
    public Map<Integer, Double> calculateSimilarities(String clientQuery, List<PredefinedAnswer> potentialAnswers) throws EmbeddingException {
        if (potentialAnswers == null || potentialAnswers.isEmpty()) {
            log.debug("Получен пустой или null список потенциальных ответов для расчета сходства.");
            return new HashMap<>();
        }

        if (clientQuery == null || clientQuery.trim().isEmpty()) {
            log.warn("Получен пустой или null запрос клиента для расчета сходства.");

            Map<Integer, Double> zeroSimilarityMap = new HashMap<>();
            for (PredefinedAnswer answer : potentialAnswers) {
                if (answer != null && answer.getId() != null) {
                    zeroSimilarityMap.put(answer.getId(), 0.0);
                }
            }
            return zeroSimilarityMap;
        }

        List<String> answerTexts = new ArrayList<>(potentialAnswers.size());
        List<Integer> answerIdsInOrder = new ArrayList<>(potentialAnswers.size());

        for (PredefinedAnswer answer : potentialAnswers) {
            if (answer != null && answer.getId() != null && answer.getAnswer() != null && !answer.getAnswer().trim().isEmpty()) {
                answerTexts.add(answer.getAnswer());
                answerIdsInOrder.add(answer.getId());
            } else {
                log.warn("Пропущен некорректный PredefinedAnswer (ID: {}, текст: {}) при подготовке текстов для расчета сходства.",
                        answer != null ? answer.getId() : "null",
                        (answer != null && answer.getAnswer() != null) ? "\"" + answer.getAnswer() + "\"" : "null");

                //TODO можно по круче обработать
            }
        }

        if (answerTexts.isEmpty()) {
            log.debug("После фильтрации ни у одного потенциального ответа нет текста для расчета сходства. ");
            return new HashMap<>();
        }

        List<Float> similarityScores;
        try {
            similarityScores = sentenceSimilarityClient.getSimilarities(clientQuery, answerTexts);
        } catch (EmbeddingException e) {
            log.error("Ошибка при вызове SentenceSimilarityClient.", e);
            throw e;
        }

        if (similarityScores.size() != answerTexts.size()) {
            log.error("Несоответствие количества текстов, отправленных в SentenceSimilarityClient ({}), и количества полученных оценок ({}).",
                    answerTexts.size(), similarityScores.size());
            throw new EmbeddingException("SentenceSimilarityClient вернул неожиданное количество оценок сходства.");
        }

        Map<Integer, Double> similarityMap = new HashMap<>();
        for (int i = 0; i < similarityScores.size(); i++) {
            Integer originalAnswerId = answerIdsInOrder.get(i);
            Double score = similarityScores.get(i).doubleValue();
            similarityMap.put(originalAnswerId, score);
        }

        return similarityMap;
    }
}
