package com.example.domain.api.chat_service_api.exception_handler;

public class VkApiException extends RuntimeException {

  public VkApiException() {
    super();
  }

  public VkApiException(String message) {
        super(message);
    }

  public VkApiException(String message, Throwable cause) {
    super(message, cause);
  }

  public VkApiException(Throwable cause) {
    super(cause);
  }

  protected VkApiException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
