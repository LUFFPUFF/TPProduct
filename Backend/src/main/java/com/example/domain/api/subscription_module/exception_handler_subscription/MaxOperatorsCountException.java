package com.example.domain.api.subscription_module.exception_handler_subscription;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.BAD_REQUEST, reason = "Максимальное число операторов")
public class MaxOperatorsCountException extends RuntimeException {
  public MaxOperatorsCountException() {

  }
}
