package com.example.domain.api.ans_api_module.answer_finder.exception;

public class AnswerSearchException extends RuntimeException {

  public AnswerSearchException(String message) {
    super(message);
  }

  public AnswerSearchException(String message, Throwable cause) {
    super(message, cause);
  }
}
