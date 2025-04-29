package com.example.domain.api.ans_api_module.answer_finder.config;

import com.example.domain.api.ans_api_module.answer_finder.nlp.hf_similarity_client.SentenceSimilarityClient;
import com.example.domain.api.ans_api_module.answer_finder.search.AnswerMatcher;
import com.example.domain.api.ans_api_module.answer_finder.search.ranker.AnswerRanker;
import com.example.domain.api.ans_api_module.answer_finder.search.ranker.ScoreBasedAnswerRanker;
import com.example.domain.api.ans_api_module.answer_finder.search.score_combiner.ScoreCombiner;
import com.example.domain.api.ans_api_module.answer_finder.search.score_combiner.WeightedSumScoreCombiner;
import com.example.domain.api.ans_api_module.answer_finder.search.similarity_calculator.EmbeddingSimilarityCalculator;
import com.example.domain.api.ans_api_module.answer_finder.search.similarity_calculator.SimilarityCalculator;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(SearchProperties.class)
public class SearchConfig {

    @Bean
    public ScoreCombiner weightedSumScoreCombiner(SearchProperties searchProperties) {
        return new WeightedSumScoreCombiner(searchProperties.getSimilarity(), searchProperties.getTrust());
    }

    @Bean
    public SimilarityCalculator embeddingSimilarityCalculator(SentenceSimilarityClient huggingFaceSentenceSimilarityClient) {
        return new EmbeddingSimilarityCalculator(huggingFaceSentenceSimilarityClient);
    }

    @Bean
    public AnswerRanker scoreBasedAnswerRanker() {
        return new ScoreBasedAnswerRanker();
    }

    @Bean
    public AnswerMatcher answerMatcher(
            SimilarityCalculator similarityCalculator,
            ScoreCombiner scoreCombiner,
            AnswerRanker answerRanker) {
        return new AnswerMatcher(similarityCalculator, scoreCombiner, answerRanker);
    }


}
