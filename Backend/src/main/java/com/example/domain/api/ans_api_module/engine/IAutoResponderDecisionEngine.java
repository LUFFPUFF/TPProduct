package com.example.domain.api.ans_api_module.engine;

import com.example.domain.api.ans_api_module.model.AutoResponderResult;

public interface IAutoResponderDecisionEngine {

    /**
     * Пытается найти или сгенерировать ответ на запрос клиента.
     *
     * @param clientQuery        Запрос клиента.
     * @param companyId          ID компании.
     * @param companyDescription Описание компании (для генерации общего ответа).
     * @return {@link AutoResponderResult} с результатом обработки.
     */
    AutoResponderResult decideResponse(String clientQuery, Integer companyId, String companyDescription, String clientChatHistoryForStyle);
}
