package com.example.domain.api.company_module.exception_handler_company;

import com.example.domain.exception.AbstractException;
import org.springframework.http.HttpStatus;

public class UserNotInCompanyException extends AbstractException {

    public UserNotInCompanyException() {
        super("Пользователь не принадлежит компании", HttpStatus.BAD_REQUEST);
    }
}
