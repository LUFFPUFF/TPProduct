package com.example.domain.api.subscription_module.exception_handler_subscription;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.NOT_FOUND,reason = "Данные подписки не найдены")
public class NotFoundSubscriptionException extends RuntimeException {
    public NotFoundSubscriptionException() {
    }
}
