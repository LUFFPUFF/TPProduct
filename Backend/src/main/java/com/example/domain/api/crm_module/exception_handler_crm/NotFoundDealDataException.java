package com.example.domain.api.crm_module.exception_handler_crm;

import com.example.domain.exception.AbstractException;
import org.springframework.http.HttpStatus;

public class NotFoundDealDataException extends AbstractException {

    public NotFoundDealDataException() {
        super("Не найдены данные сделки", HttpStatus.NOT_FOUND);
    }
}
