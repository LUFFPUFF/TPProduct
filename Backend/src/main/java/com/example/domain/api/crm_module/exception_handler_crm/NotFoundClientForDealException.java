package com.example.domain.api.crm_module.exception_handler_crm;

import com.example.domain.exception.AbstractException;
import org.springframework.http.HttpStatus;

public class NotFoundClientForDealException extends AbstractException {

    public NotFoundClientForDealException() {
        super("Не найден клиент для сделки", HttpStatus.NOT_FOUND);
    }
}
