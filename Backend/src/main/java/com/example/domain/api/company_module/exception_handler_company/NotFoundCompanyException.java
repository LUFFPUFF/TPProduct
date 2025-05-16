package com.example.domain.api.company_module.exception_handler_company;

import com.example.domain.exception.AbstractException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

public class NotFoundCompanyException extends AbstractException {

    public NotFoundCompanyException() {
        super("Компания не найдена", HttpStatus.NOT_FOUND);
    }
}
