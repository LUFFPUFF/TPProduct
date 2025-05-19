package com.example.domain.api.authentication_module.exception_handler_auth;

import com.example.domain.exception.AbstractException;
import org.springframework.http.HttpStatus;

public class InvalidCodeException extends AbstractException {

    public InvalidCodeException() {
        super("Неверный код", HttpStatus.BAD_REQUEST);
    }
}
