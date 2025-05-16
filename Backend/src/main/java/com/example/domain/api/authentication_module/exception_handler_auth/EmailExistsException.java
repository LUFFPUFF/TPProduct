package com.example.domain.api.authentication_module.exception_handler_auth;

import com.example.domain.exception.AbstractException;
import org.springframework.http.HttpStatus;

public class EmailExistsException extends AbstractException {

    public EmailExistsException() {
        super("Ошибка авторизации: Пользователь с данной почтой уже зарегистрирован", HttpStatus.BAD_REQUEST);
    }
}
