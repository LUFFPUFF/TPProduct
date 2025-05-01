package com.example.domain.api.ans_api_module.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class AutoResponderEscalationEvent extends ApplicationEvent {

    private final Integer chatId;
    private final Integer clientId;

    /**
     * @param source Объект, который опубликовал событие (обычно 'this').
     * @param chatId ID чата, который нужно эскалировать.
     * @param clientId ID клиента, который запросил эскалацию (для проверки прав в слушателе).
     */
    public AutoResponderEscalationEvent(Object source, Integer chatId, Integer clientId) {
        super(source);
        this.chatId = chatId;
        this.clientId = clientId;
    }

}
