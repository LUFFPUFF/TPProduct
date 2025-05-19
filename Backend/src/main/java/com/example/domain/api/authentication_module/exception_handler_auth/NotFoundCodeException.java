package com.example.domain.api.authentication_module.exception_handler_auth;

import com.example.domain.exception.AbstractException;
import org.springframework.http.HttpStatus;

public class NotFoundCodeException extends AbstractException {

    public NotFoundCodeException() {
        super("Код не найден", HttpStatus.BAD_REQUEST);
    }
}
