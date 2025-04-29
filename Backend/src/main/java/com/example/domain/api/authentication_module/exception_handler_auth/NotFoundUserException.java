package com.example.domain.api.authentication_module.exception_handler_auth;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.I_AM_A_TEAPOT, reason = "Аккаунт не найден")
public class NotFoundUserException extends RuntimeException {
    public NotFoundUserException() {
    }
}
