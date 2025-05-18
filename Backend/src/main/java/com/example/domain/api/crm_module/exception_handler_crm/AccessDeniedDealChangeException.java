package com.example.domain.api.crm_module.exception_handler_crm;

import com.example.domain.exception.AbstractException;
import org.springframework.http.HttpStatus;

public class AccessDeniedDealChangeException extends AbstractException {

    public AccessDeniedDealChangeException() {
        super("У вас нет прав для изменения сделки", HttpStatus.FORBIDDEN);
    }
}
