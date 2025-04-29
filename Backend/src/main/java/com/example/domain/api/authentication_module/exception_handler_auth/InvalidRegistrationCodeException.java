package com.example.domain.api.authentication_module.exception_handler_auth;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.BAD_REQUEST, reason = "Код не найден")
public class InvalidRegistrationCodeException extends RuntimeException {
    public InvalidRegistrationCodeException() {

    }
}
