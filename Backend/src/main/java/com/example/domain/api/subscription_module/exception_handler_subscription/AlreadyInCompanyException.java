package com.example.domain.api.subscription_module.exception_handler_subscription;

import com.example.domain.exception.AbstractException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

public class AlreadyInCompanyException extends AbstractException {

  public AlreadyInCompanyException() {
    super("Подписка невозможна пока вы находитесь в компании", HttpStatus.BAD_REQUEST);
  }
}
