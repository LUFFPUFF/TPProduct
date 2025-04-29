package com.example.domain.api.ans_api_module.answer_finder.nlp.lemmatizer.impl;

import com.example.domain.api.ans_api_module.answer_finder.exception.NlpException;
import com.example.domain.api.ans_api_module.answer_finder.nlp.lemmatizer.Lemmatizer;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class LocalLemmatizer implements Lemmatizer {


    @Override
    public String lemmatize(String word) throws NlpException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<String> lemmatize(List<String> words) throws NlpException {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
