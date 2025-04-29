package com.example.domain.api.company_module.exception_handler_company;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.BAD_REQUEST, reason = "Нельзя убрать себя из участников компании")
public class SelfMemberDisbandException extends RuntimeException {
    public SelfMemberDisbandException() {

    }
}
