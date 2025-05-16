package com.example.domain.api.subscription_module.exception_handler_subscription;

import com.example.domain.exception.AbstractException;
import org.springframework.http.HttpStatus;


public class SubtractOperatorException extends AbstractException {


    public SubtractOperatorException() {
        super("число операторов не может быть меньше 1", HttpStatus.BAD_REQUEST);
    }
}

