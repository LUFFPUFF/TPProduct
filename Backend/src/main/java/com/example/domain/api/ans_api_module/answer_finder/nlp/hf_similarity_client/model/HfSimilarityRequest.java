package com.example.domain.api.ans_api_module.answer_finder.nlp.hf_similarity_client.model;

import lombok.Data;

import java.util.List;


@Data
public class HfSimilarityRequest {

    private SentenceSimilarityInput inputs;

    public HfSimilarityRequest() {}

    public static HfSimilarityRequest from(String sourceSentence, List<String> sentences) {
        HfSimilarityRequest request = new HfSimilarityRequest();
        request.setInputs(new SentenceSimilarityInput(sourceSentence, sentences));
        return request;
    }


    @Data
    private static class SentenceSimilarityInput {

        private String source_sentence;
        private List<String> sentences;

        public SentenceSimilarityInput() {}
        public SentenceSimilarityInput(String sourceSentence, List<String> sentences) {
            this.source_sentence = sourceSentence;
            this.sentences = sentences;
        }
    }
}
