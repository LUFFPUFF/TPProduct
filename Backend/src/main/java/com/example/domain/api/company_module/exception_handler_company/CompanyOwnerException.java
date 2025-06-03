package com.example.domain.api.company_module.exception_handler_company;

import com.example.domain.exception.AbstractException;
import org.springframework.http.HttpStatus;

public class CompanyOwnerException extends AbstractException {

    public CompanyOwnerException() {
        super("Нельзя изменять права владельца компании", HttpStatus.FORBIDDEN);
    }
}

