package com.example.domain.api.ans_api_module.answer_finder.nlp.lemmatizer;

import com.example.domain.api.ans_api_module.answer_finder.exception.NlpException;

import java.util.List;

public interface Lemmatizer {

    /**
     * Выполнить лемматизацию одного слова.
     * @param word Входное слово.
     * @return Лемма слова.
     * @throws NlpException В случае ошибки при лемматизации.
     */
    String lemmatize(String word) throws NlpException;

    /**
     * Выполнить лемматизацию списка слов.
     * Может быть более эффективным при работе с батчами.
     * @param words Список входных слов.
     * @return Список лемм для каждого слова.
     * @throws NlpException В случае ошибки при лемматизации.
     */
    List<String> lemmatize(List<String> words) throws NlpException;
}
