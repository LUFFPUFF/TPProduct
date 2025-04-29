package com.example.domain.api.ans_api_module.answer_finder.nlp.hf_similarity_client;

import com.example.domain.api.ans_api_module.answer_finder.exception.EmbeddingException;

import java.util.List;

public interface SentenceSimilarityClient {

    /**
     * Получить оценки сходства между исходным предложением и списком других предложений.
     * @param sourceSentence Исходное предложение.
     * @param sentences Список предложений для сравнения.
     * @return Список оценок сходства (float) для каждого предложения из списка sentences.
     * @throws EmbeddingException В случае ошибки взаимодействия с API.
     */
    List<Float> getSimilarities(String sourceSentence, List<String> sentences) throws EmbeddingException;
}
