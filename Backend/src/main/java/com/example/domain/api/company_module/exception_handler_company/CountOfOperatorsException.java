package com.example.domain.api.company_module.exception_handler_company;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.BAD_REQUEST,reason = "Тарифный план не позволяет добавить участника")
public class CountOfOperatorsException extends RuntimeException {
    public CountOfOperatorsException(String message) {
        super(message);
    }
}
