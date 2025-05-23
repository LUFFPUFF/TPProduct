package com.example.domain.api.company_module.exception_handler_company;

import com.example.domain.exception.AbstractException;
import org.springframework.http.HttpStatus;

public class UserAlreadyInCompanyExeption extends AbstractException {

    public UserAlreadyInCompanyExeption() {
        super("Пользователь уже в компании", HttpStatus.BAD_REQUEST);
    }
}
