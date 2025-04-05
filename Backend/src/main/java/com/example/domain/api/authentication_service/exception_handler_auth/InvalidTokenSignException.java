package com.example.domain.api.authentication_service.exception_handler_auth;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.UNAUTHORIZED, reason = "Ошибка аутентификации")
public class InvalidTokenSignException extends RuntimeException {
    public InvalidTokenSignException() {
    }
}
