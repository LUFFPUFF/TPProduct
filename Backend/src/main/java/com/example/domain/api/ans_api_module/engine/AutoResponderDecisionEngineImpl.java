package com.example.domain.api.ans_api_module.engine;

import com.example.domain.api.ans_api_module.answer_finder.dto.AnswerSearchResultItem;
import com.example.domain.api.ans_api_module.answer_finder.exception.AnswerSearchException;
import com.example.domain.api.ans_api_module.answer_finder.service.AnswerSearchService;
import com.example.domain.api.ans_api_module.generation.exception.MLException;
import com.example.domain.api.ans_api_module.generation.model.enums.GenerationType;
import com.example.domain.api.ans_api_module.generation.service.ITextGenerationService;
import com.example.domain.api.ans_api_module.model.AutoResponderResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AutoResponderDecisionEngineImpl implements IAutoResponderDecisionEngine {

    private final AnswerSearchService answerSearchService;
    private final ITextGenerationService textGenerationService;

    private static final double RELEVANCE_THRESHOLD = 0.6;

    public static final String MSG_KEY_ESCALATE_NO_ANSWER = "autoresponder.escalate.no_answer";
    public static final String MSG_KEY_ESCALATE_SEARCH_ERROR = "autoresponder.escalate.search_error";
    public static final String MSG_KEY_ESCALATE_GENERATION_ERROR = "autoresponder.escalate.generation_error";
    public static final String MSG_KEY_ESCALATE_REWRITE_ERROR = "autoresponder.escalate.rewrite_error";


    @Override
    public AutoResponderResult decideResponse(String clientQuery, Integer companyId, String companyDescription, String clientChatHistoryForStyle) {
        String correctedQuery = null;
        String analyzedClientStyle = null;

        if (StringUtils.hasText(clientChatHistoryForStyle)) {
            try {
                analyzedClientStyle = textGenerationService.analyzeClientStyle(clientChatHistoryForStyle);
            } catch (MLException e) {
                log.warn("DecisionEngine: Failed to analyze client style. Proceeding without style adaptation. Error: {}", e.getMessage());
            } catch (Exception e) {
                log.error("DecisionEngine: Unexpected error during client style analysis. Error: {}", e.getMessage(), e);
            }
        }

        try {
            correctedQuery = textGenerationService.processQuery(clientQuery, GenerationType.CORRECTION, null);
            log.debug("DecisionEngine: Corrected query: {}", correctedQuery);
        } catch (MLException e) {
            log.warn("DecisionEngine: Failed to correct query, using original query ('{}'). Error: {}", clientQuery, e.getMessage());
            correctedQuery = clientQuery;
        } catch (Exception e) {
            log.error("DecisionEngine: Unexpected error during query correction, using original query ('{}'). Error: {}", clientQuery, e.getMessage(), e);
            correctedQuery = clientQuery;
        }

        try {
            List<AnswerSearchResultItem> relevantAnswers = answerSearchService.findRelevantAnswers(correctedQuery, companyId, null);
            Optional<AnswerSearchResultItem> bestAnswerOpt = relevantAnswers.stream().findFirst();

            if (bestAnswerOpt.isPresent() && bestAnswerOpt.get().getScore() >= RELEVANCE_THRESHOLD) {
                String originalAnswerText = bestAnswerOpt.get().getAnswer().getAnswer();
                try {
                    String rewrittenAnswer = textGenerationService.processQuery(originalAnswerText, GenerationType.REWRITE, analyzedClientStyle != null ? analyzedClientStyle : clientChatHistoryForStyle);
                    return AutoResponderResult.builder()
                            .answerProvided(true)
                            .responseText(rewrittenAnswer)
                            .requiresEscalation(false)
                            .isPurelyGenerated(false)
                            .confidence((float) bestAnswerOpt.get().getScore())
                            .build();
                } catch (MLException e) {
                    log.warn("DecisionEngine: Failed to rewrite predefined answer. Error: {}. Escalating.", e.getMessage());
                    return AutoResponderResult.builder().requiresEscalation(true).escalationReasonMessageKey(MSG_KEY_ESCALATE_REWRITE_ERROR).build();
                }
            }
        } catch (AnswerSearchException e) {
            log.error("DecisionEngine: Error during answer search: {}. Escalating.", e.getMessage(), e);
            return AutoResponderResult.builder().requiresEscalation(true).escalationReasonMessageKey(MSG_KEY_ESCALATE_SEARCH_ERROR).build();
        }

        if (companyDescription != null && !companyDescription.trim().isEmpty()) {
            try {
                String generatedAnswer = textGenerationService.generateGeneralAnswer(correctedQuery, companyDescription, analyzedClientStyle != null ? analyzedClientStyle : clientChatHistoryForStyle);
                if (generatedAnswer != null && !generatedAnswer.trim().isEmpty()) {
                    return AutoResponderResult.builder()
                            .answerProvided(true)
                            .responseText(generatedAnswer)
                            .requiresEscalation(false)
                            .isPurelyGenerated(true)
                            .build();
                } else {
                    log.warn("DecisionEngine: General answer generation returned empty/null. Escalating.");
                    return AutoResponderResult.builder().requiresEscalation(true).escalationReasonMessageKey(MSG_KEY_ESCALATE_NO_ANSWER).build();
                }
            } catch (MLException e) {
                log.warn("DecisionEngine: Failed to generate general answer. Error: {}. Escalating.", e.getMessage(), e);
                return AutoResponderResult.builder().requiresEscalation(true).escalationReasonMessageKey(MSG_KEY_ESCALATE_GENERATION_ERROR).build();
            }
        } else {
            log.warn("DecisionEngine: Company description is missing. Cannot generate general answer. Escalating.");
            return AutoResponderResult.builder().requiresEscalation(true).escalationReasonMessageKey(MSG_KEY_ESCALATE_NO_ANSWER).build();
        }

    }
}
