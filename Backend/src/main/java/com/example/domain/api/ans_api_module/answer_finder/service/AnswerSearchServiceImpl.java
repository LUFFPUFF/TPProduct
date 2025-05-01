package com.example.domain.api.ans_api_module.answer_finder.service;

import com.example.database.model.ai_module.PredefinedAnswer;
import com.example.database.repository.ai_module.PredefinedAnswerRepository;
import com.example.domain.api.ans_api_module.answer_finder.config.SearchProperties;
import com.example.domain.api.ans_api_module.answer_finder.dto.AnswerSearchResultItem;
import com.example.domain.api.ans_api_module.answer_finder.exception.AnswerSearchException;
import com.example.domain.api.ans_api_module.answer_finder.exception.NlpException;
import com.example.domain.api.ans_api_module.answer_finder.mapper.AnswerSearchMapper;
import com.example.domain.api.ans_api_module.answer_finder.nlp.lemmatizer.Lemmatizer;
import com.example.domain.api.ans_api_module.answer_finder.search.AnswerMatcher;
import com.example.domain.api.ans_api_module.answer_finder.search.dto.ScoredAnswerCandidate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;


@RequiredArgsConstructor
@Slf4j
@Service
public class AnswerSearchServiceImpl implements AnswerSearchService {

    private final AnswerMatcher answerMatcher;
    private final PredefinedAnswerRepository answerRepository;
    private final AnswerSearchMapper answerSearchMapper;
    private final SearchProperties searchProperties;
    private final Lemmatizer lemmatizer;

    @Override
    public List<AnswerSearchResultItem> findRelevantAnswers(String clientQuery, Integer companyId, String category) throws AnswerSearchException {
        log.info("Сервис поиска ответов: запрос '{}', companyId={}, category={}",
                truncateLogString(clientQuery), companyId, category);

        if (clientQuery == null || clientQuery.trim().isEmpty()) {
            log.warn("Запрос клиента пуст или null. Невозможно выполнить поиск. Возвращаем пустой список.");
            return Collections.emptyList();
        }
        if (clientQuery.length() > 1000) {
            log.warn("Запрос клиента очень длинный ({} символов). Может повлиять на производительность API.", clientQuery.length());
        }

        String processedQuery = clientQuery;

        //TODO не работает лематизация
//        try {
//            List<String> queryLemmas = lemmatizer.lemmatize(Collections.singletonList(clientQuery));
//            if (queryLemmas != null && !queryLemmas.isEmpty()) {
//                processedQuery = String.join(" ", queryLemmas);
//                log.debug("Лемматизированный запрос клиента: '{}'", processedQuery);
//            } else {
//                log.warn("Лемматизатор вернул пустой список лемм для запроса '{}'. Использование оригинального запроса.",
//                        truncateLogString(clientQuery));
//                processedQuery = clientQuery;
//            }
//        } catch (NlpException e) {
//            log.warn("Ошибка при лемматизации запроса клиента '{}'. Использование оригинального запроса.",
//                    truncateLogString(clientQuery), e);
//            processedQuery = clientQuery;
//        }

        List<PredefinedAnswer> potentialAnswers = fetchPotentialAnswers(companyId, category);

        if (potentialAnswers == null || potentialAnswers.isEmpty()) {
            log.info("Из репозитория не получено потенциальных ответов. Возвращаем пустой список.");
            return Collections.emptyList();
        }

        List<ScoredAnswerCandidate> rankedCandidates;
        try {
            rankedCandidates = answerMatcher.findBestAnswers(processedQuery, potentialAnswers);
        } catch (AnswerSearchException e) {
            log.error("Ошибка при выполнении AnswerMatcher.", e);
            throw e;
        } catch (Exception e) {
            log.error("Неожиданная ошибка при выполнении AnswerMatcher.", e);
            throw new AnswerSearchException("Неожиданная ошибка во время поиска ответов.", e);
        }

        if (rankedCandidates == null || rankedCandidates.isEmpty()) {
            log.info("AnswerMatcher не вернул ранжированных кандидатов. Возвращаем пустой список.");
            return Collections.emptyList();
        }

        List<ScoredAnswerCandidate> filteredAndLimitedCandidates = rankedCandidates.stream()
                .filter(candidate -> candidate.getCombinedScore() >= searchProperties.getMinCombinedScoreThreshold())
                .limit(searchProperties.getMaxResults())
                .toList();

        return answerSearchMapper.toAnswerSearchResultItemList(filteredAndLimitedCandidates);
    }

    private List<PredefinedAnswer> fetchPotentialAnswers(Integer companyId, String category) {
//        if (companyId != null && category != null) {
//            return answerRepository.findByCompanyIdAndCategory(companyId, category);
//        } else if (companyId != null) {
//            return answerRepository.findByCompanyId(companyId);
//        } else if (category != null) {
//            return answerRepository.findByCategory(category);
//        } else {
//            log.warn("Выборка всех предопределенных ответов без фильтров (companyId/category).");
//            return answerRepository.findAll();
//        }

        //TODO на момент проверки пока что возвращаются все ответы по companyID
        return answerRepository.findByCompanyId(companyId);
    }

    private String truncateLogString(String text) {
        if (text == null) {
            return "null";
        }
        if (text.length() <= 100) {
            return text;
        }
        return text.substring(0, 100) + "...";
    }
}
