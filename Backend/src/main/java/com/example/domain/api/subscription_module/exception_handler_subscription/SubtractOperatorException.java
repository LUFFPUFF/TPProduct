package com.example.domain.api.subscription_module.exception_handler_subscription;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.BAD_REQUEST,reason = "число операторов не может быть меньше 1")
public class SubtractOperatorException extends RuntimeException {
    public SubtractOperatorException() {

    }
}
