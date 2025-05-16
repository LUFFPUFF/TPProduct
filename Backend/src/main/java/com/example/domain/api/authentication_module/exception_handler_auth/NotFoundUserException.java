package com.example.domain.api.authentication_module.exception_handler_auth;

import com.example.domain.exception.AbstractException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

public class NotFoundUserException extends AbstractException {

    public NotFoundUserException() {
        super("Аккаунт не найден", HttpStatus.NOT_FOUND);
    }
}
