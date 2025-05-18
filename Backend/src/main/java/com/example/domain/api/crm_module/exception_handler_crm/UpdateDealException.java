package com.example.domain.api.crm_module.exception_handler_crm;

import com.example.domain.exception.AbstractException;
import org.springframework.http.HttpStatus;

public class UpdateDealException extends AbstractException {


    public UpdateDealException(String message, HttpStatus status) {
        super(message, status);
    }
    public UpdateDealException() {
        super("Не удалось обновить данные сделки", HttpStatus.BAD_REQUEST);
    }
}
