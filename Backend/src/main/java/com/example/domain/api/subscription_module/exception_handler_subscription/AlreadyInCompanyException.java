package com.example.domain.api.subscription_module.exception_handler_subscription;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.BAD_REQUEST,reason = "Подписка невозможна пока вы находитесь в компании")
public class AlreadyInCompanyException extends RuntimeException {
  public AlreadyInCompanyException() {}
}
