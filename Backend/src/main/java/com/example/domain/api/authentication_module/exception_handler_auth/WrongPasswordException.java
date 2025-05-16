package com.example.domain.api.authentication_module.exception_handler_auth;

import com.example.domain.exception.AbstractException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;


public class WrongPasswordException extends AbstractException {

    public WrongPasswordException() {
        super("Ошибка аутентификации: Неверный пароль", HttpStatus.UNAUTHORIZED);
    }
}
