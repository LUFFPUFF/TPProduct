package com.example.domain.api.statistics_module.exception;

public class StatisticsQueryException extends RuntimeException {

    public StatisticsQueryException() {
    }

    public StatisticsQueryException(String message) {
        super(message);
    }

    public StatisticsQueryException(String message, Throwable cause) {
        super(message, cause);
    }

    public StatisticsQueryException(Throwable cause) {
        super(cause);
    }

    public StatisticsQueryException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
