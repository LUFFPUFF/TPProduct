package com.example.domain.api.crm_module.exception_handler_crm;

import com.example.domain.exception.AbstractException;
import org.springframework.http.HttpStatus;

public class AccessDeniedFilterArchive extends AbstractException {
    public AccessDeniedFilterArchive() {
        super("У вас нет прав для просмотра архива по данному запросу", HttpStatus.FORBIDDEN);
    }
}
