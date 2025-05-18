package com.example.domain.api.crm_module.exception_handler_crm;

import com.example.domain.exception.AbstractException;
import org.springframework.http.HttpStatus;

public class NotFoundDealStageException extends AbstractException {

    public NotFoundDealStageException() {
        super("Не найден этап сделки", HttpStatus.NOT_FOUND);
    }
}
