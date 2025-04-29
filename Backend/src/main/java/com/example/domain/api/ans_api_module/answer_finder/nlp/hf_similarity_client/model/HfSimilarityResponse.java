package com.example.domain.api.ans_api_module.answer_finder.nlp.hf_similarity_client.model;


import lombok.Data;

import java.util.List;

@Data
public class HfSimilarityResponse {

    private List<List<Float>> embeddings;
}
