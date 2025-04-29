package com.example.domain.api.authentication_module.exception_handler_auth;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.BAD_REQUEST, reason = "Ошибка авторизации: Пользователь с данной почтой уже зарегистрирован")
public class EmailExistsException extends RuntimeException {
    public EmailExistsException() {
    }
}
