package com.example.domain.api.ans_api_module.answer_finder.service;

import com.example.domain.api.ans_api_module.answer_finder.dto.AnswerSearchResultItem;
import com.example.domain.api.ans_api_module.answer_finder.exception.AnswerSearchException;
import org.jvnet.hk2.annotations.Service;

import java.util.List;

public interface AnswerSearchService {

    /**
     * Выполняет поиск наиболее релевантных предопределенных ответов для заданного запроса клиента
     * с возможностью фильтрации по компании и категории.
     * <p>
     * Включает в себя получение потенциальных ответов, их предобработку (лемматизацию запроса),
     * расчет оценок релевантности (сходство + TrustScore), ранжирование и постобработку (фильтрация, лимит).
     *
     * @param clientQuery Текст запроса клиента. Должен быть не null и не пустым после первичной валидации.
     * @param companyId   ID компании для фильтрации. Может быть null, если фильтрация по компании не требуется.
     * @param category    Категория ответов для фильтрации. Может быть null, если фильтрация по категории не требуется.
     * @return Список наиболее релевантных ответов, отсортированный по убыванию релевантности,
     *         в формате DTO для API {@link AnswerSearchResultItem}.
     *         Возвращает пустой список, если ответы не найдены по фильтрам,
     *         или если после ранжирования и постобработки не осталось подходящих кандидатов,
     *         или если запрос клиента некорректен.
     * @throws AnswerSearchException В случае ошибки во время выполнения бизнес-логики поиска
     *                               (например, при сбое внешнего API, ошибке в логике поиска и т.п.).
     */
    List<AnswerSearchResultItem> findRelevantAnswers(
            String clientQuery,
            Integer companyId,
            String category) throws AnswerSearchException;
}
