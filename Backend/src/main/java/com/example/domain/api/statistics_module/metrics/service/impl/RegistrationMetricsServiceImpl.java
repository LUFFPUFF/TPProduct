package com.example.domain.api.statistics_module.metrics.service.impl;

import com.example.domain.api.statistics_module.metrics.service.IRegistrationMetricsService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RegistrationMetricsServiceImpl implements IRegistrationMetricsService {

    private final MeterRegistry registry;
    private static final String METRIC_PREFIX = "registration_app_";

    private static final String REG_ATTEMPT_TOTAL = METRIC_PREFIX + "attempt_total";
    private static final String REG_CODE_SENT_SUCCESS_TOTAL = METRIC_PREFIX + "code_sent_success_total";
    private static final String REG_CODE_SENT_FAILURE_TOTAL = METRIC_PREFIX + "code_sent_failure_total";
    private static final String REG_CODE_CHECK_SUCCESS_TOTAL = METRIC_PREFIX + "code_check_success_total";
    private static final String REG_CODE_CHECK_FAILURE_INVALID_TOTAL = METRIC_PREFIX + "code_check_failure_invalid_total";
    private static final String REG_EMAIL_EXISTS_TOTAL = METRIC_PREFIX + "email_exists_total";
    private static final String REG_OPERATION_ERRORS_TOTAL = METRIC_PREFIX + "operation_errors_total";

    private static final String TAG_OPERATION_NAME = "operation";
    private static final String TAG_ERROR_TYPE = "error_type";

    @Override
    public void incrementUserRegistrationAttempt() {
        Counter.builder(REG_ATTEMPT_TOTAL).register(registry).increment();
    }

    @Override
    public void incrementRegistrationCodeSentSuccess() {
        Counter.builder(REG_CODE_SENT_SUCCESS_TOTAL).register(registry).increment();
    }

    @Override
    public void incrementRegistrationCodeSentFailure() {
        Counter.builder(REG_CODE_SENT_FAILURE_TOTAL).register(registry).increment();
    }

    @Override
    public void incrementRegistrationCodeCheckSuccess() {
        Counter.builder(REG_CODE_CHECK_SUCCESS_TOTAL).register(registry).increment();
    }

    @Override
    public void incrementRegistrationCodeCheckFailureInvalidCode() {
        Counter.builder(REG_CODE_CHECK_FAILURE_INVALID_TOTAL).register(registry).increment();
    }

    @Override
    public void incrementRegistrationEmailExists() {
        Counter.builder(REG_EMAIL_EXISTS_TOTAL).register(registry).increment();
    }

    @Override
    public void incrementRegistrationOperationError(String operationName, String errorType) {
        Counter.builder(REG_OPERATION_ERRORS_TOTAL)
                .tag(TAG_OPERATION_NAME, operationName)
                .tag(TAG_ERROR_TYPE, errorType)
                .register(registry)
                .increment();
    }
}
