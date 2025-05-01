package com.example.domain.api.company_module.exception_handler_company;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.NOT_FOUND,reason = "Компания не найдена")
public class NotFoundCompanyException extends RuntimeException {
    public NotFoundCompanyException() {

    }
}
