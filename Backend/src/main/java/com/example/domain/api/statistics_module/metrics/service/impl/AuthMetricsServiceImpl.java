package com.example.domain.api.statistics_module.metrics.service.impl;

import com.example.domain.api.statistics_module.metrics.service.IAuthMetricsService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthMetricsServiceImpl implements IAuthMetricsService {

    private final MeterRegistry registry;
    private static final String METRIC_PREFIX = "auth_app_";

    private static final String LOGIN_SUCCESS_TOTAL = METRIC_PREFIX + "login_success_total";
    private static final String LOGIN_FAILURE_USER_NOT_FOUND_TOTAL = METRIC_PREFIX + "login_failure_user_not_found_total";
    private static final String LOGIN_FAILURE_WRONG_PASSWORD_TOTAL = METRIC_PREFIX + "login_failure_wrong_password_total";
    private static final String LOGOUT_SUCCESS_TOTAL = METRIC_PREFIX + "logout_success_total";
    private static final String AUTH_OPERATION_ERRORS_TOTAL = METRIC_PREFIX + "operation_errors_total";

    private static final String TAG_OPERATION_NAME = "operation";
    private static final String TAG_ERROR_TYPE = "error_type";

    @Override
    public void incrementLoginSuccess() {
        Counter.builder(LOGIN_SUCCESS_TOTAL)
                .description("Общее количество успешных входов в систему")
                .register(registry)
                .increment();
    }

    @Override
    public void incrementLoginFailureUserNotFound() {
        Counter.builder(LOGIN_FAILURE_USER_NOT_FOUND_TOTAL)
                .description("Общее количество ошибок входа в систему из-за того, что пользователь не найден")
                .register(registry)
                .increment();
    }

    @Override
    public void incrementLoginFailureWrongPassword() {
        Counter.builder(LOGIN_FAILURE_WRONG_PASSWORD_TOTAL)
                .description("Total login failures due to wrong password")
                .register(registry)
                .increment();
    }

    @Override
    public void incrementLogoutSuccess() {
        Counter.builder(LOGOUT_SUCCESS_TOTAL)
                .description("Общее количество сбоев при входе в систему из-за неправильного пароля")
                .register(registry)
                .increment();
    }

    @Override
    public void incrementAuthOperationError(String operationName, String errorType) {
        Counter.builder(AUTH_OPERATION_ERRORS_TOTAL)
                .description("Общее количество ошибок при выполнении операций аутентификации")
                .tag(TAG_OPERATION_NAME, operationName)
                .tag(TAG_ERROR_TYPE, errorType)
                .register(registry)
                .increment();
    }
}
