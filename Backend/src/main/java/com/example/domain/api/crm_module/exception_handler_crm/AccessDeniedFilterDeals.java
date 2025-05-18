package com.example.domain.api.crm_module.exception_handler_crm;

import com.example.domain.exception.AbstractException;
import org.springframework.http.HttpStatus;

public class AccessDeniedFilterDeals extends AbstractException {

    public AccessDeniedFilterDeals() {
        super("У вас нет прав для просмотра данных сделок", HttpStatus.FORBIDDEN);
    }
}
