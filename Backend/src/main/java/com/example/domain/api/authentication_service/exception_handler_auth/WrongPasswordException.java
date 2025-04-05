package com.example.domain.api.authentication_service.exception_handler_auth;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "Ошибка аутентификации: Неверный пароль")
public class WrongPasswordException extends RuntimeException {
    public WrongPasswordException() {
    }
}
