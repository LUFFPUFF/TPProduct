package com.example.domain.api.company_module.exception_handler_company;

import com.example.domain.exception.AbstractException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

public class SelfMemberDisbandException extends AbstractException {

    public SelfMemberDisbandException() {
        super("Нельзя убрать себя из участников компании", HttpStatus.BAD_REQUEST);
    }
}
