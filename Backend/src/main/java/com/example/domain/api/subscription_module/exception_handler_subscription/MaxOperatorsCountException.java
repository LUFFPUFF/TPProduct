package com.example.domain.api.subscription_module.exception_handler_subscription;

import com.example.domain.exception.AbstractException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

public class MaxOperatorsCountException extends AbstractException {

  public MaxOperatorsCountException() {
    super("Максимальное число операторов", HttpStatus.BAD_REQUEST);
  }
}
