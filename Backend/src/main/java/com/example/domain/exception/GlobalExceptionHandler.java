package com.example.domain.exception;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;


@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(AbstractException.class)
    public ResponseEntity<ExceptionDto> handler(AbstractException e) {
           return ResponseEntity.status(e.getHttpStatus()).body(ExceptionDto.builder().httpStatus(e.getHttpStatus()).message(e.getMessage()).build());
    }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ExceptionDto> handler(Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ExceptionDto.builder().message(e.getMessage()).build());
    }

}
