package com.example.domain.exception;

import org.springframework.http.HttpStatus;

public abstract class AbstractException extends RuntimeException {
    private final HttpStatus status;
    public AbstractException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }
    public HttpStatus getHttpStatus(){
        return status;
    }
}
