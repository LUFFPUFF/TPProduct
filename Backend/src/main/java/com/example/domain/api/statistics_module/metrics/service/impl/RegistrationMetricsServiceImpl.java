package com.example.domain.api.statistics_module.metrics.service.impl;

import com.example.domain.api.statistics_module.metrics.service.IRegistrationMetricsService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
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
        try {
            Counter.builder(REG_ATTEMPT_TOTAL)
                    .description("Total user registration attempts")
                    .register(registry)
                    .increment();
        } catch (Exception e) {
            log.error("METRIC_DEBUG: ERROR incrementing REG_ATTEMPT_TOTAL", e);
        }
    }

    @Override
    public void incrementRegistrationCodeSentSuccess() {
        try {
            Counter.builder(REG_CODE_SENT_SUCCESS_TOTAL)
                    .description("Total successful registration code sends")
                    .register(registry)
                    .increment();
        } catch (Exception e) {
            log.error("METRIC_DEBUG: ERROR incrementing REG_CODE_SENT_SUCCESS_TOTAL", e);
        }
    }

    @Override
    public void incrementRegistrationCodeSentFailure() {
        try {
            Counter.builder(REG_CODE_SENT_FAILURE_TOTAL)
                    .description("Total failed registration code sends")
                    .register(registry)
                    .increment();
        } catch (Exception e) {
            log.error("METRIC_DEBUG: ERROR incrementing REG_CODE_SENT_FAILURE_TOTAL", e);
        }
    }

    @Override
    public void incrementRegistrationCodeCheckSuccess() {
        try {
            Counter.builder(REG_CODE_CHECK_SUCCESS_TOTAL)
                    .description("Total successful registration code checks")
                    .register(registry)
                    .increment();
        } catch (Exception e) {
            log.error("METRIC_DEBUG: ERROR incrementing REG_CODE_CHECK_SUCCESS_TOTAL", e);
        }
    }

    @Override
    public void incrementRegistrationCodeCheckFailureInvalidCode() {
        try {
            Counter.builder(REG_CODE_CHECK_FAILURE_INVALID_TOTAL)
                    .description("Total registration code check failures due to invalid code")
                    .register(registry)
                    .increment();
        } catch (Exception e) {
            log.error("METRIC_DEBUG: ERROR incrementing REG_CODE_CHECK_FAILURE_INVALID_TOTAL", e);
        }
    }

    @Override
    public void incrementRegistrationEmailExists() {
        try {
            Counter.builder(REG_EMAIL_EXISTS_TOTAL)
                    .description("Total registration attempts with an email that already exists")
                    .register(registry)
                    .increment();
        } catch (Exception e) {
            log.error("METRIC_DEBUG: ERROR incrementing REG_EMAIL_EXISTS_TOTAL", e);
        }
    }

    @Override
    public void incrementRegistrationOperationError(String operationName, String errorType) {
        try {
            Counter.builder(REG_OPERATION_ERRORS_TOTAL)
                    .description("Total errors during registration operations")
                    .tag(TAG_OPERATION_NAME, operationName)
                    .tag(TAG_ERROR_TYPE, errorType)
                    .register(registry)
                    .increment();
        } catch (Exception e) {
            log.error("METRIC_DEBUG: ERROR incrementing REG_OPERATION_ERRORS_TOTAL. Operation: '{}', ErrorType: '{}'", operationName, errorType, e);
        }
    }
}
