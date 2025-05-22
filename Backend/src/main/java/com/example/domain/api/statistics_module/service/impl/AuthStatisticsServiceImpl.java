package com.example.domain.api.statistics_module.service.impl;

import com.example.domain.api.statistics_module.metrics.client.PrometheusQueryClient;
import com.example.domain.api.statistics_module.model.auth.AuthSummaryStatsDTO;
import com.example.domain.api.statistics_module.model.metric.StatisticsQueryRequestDTO;
import com.example.domain.api.statistics_module.service.AbstractStatisticsService;
import com.example.domain.api.statistics_module.service.IAuthStatisticsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public class AuthStatisticsServiceImpl extends AbstractStatisticsService implements IAuthStatisticsService {

    private static final String METRIC_PREFIX = "auth_app_";
    private static final String LOGIN_SUCCESS_TOTAL = METRIC_PREFIX + "login_success_total";
    private static final String LOGIN_FAILURE_USER_NOT_FOUND_TOTAL = METRIC_PREFIX + "login_failure_user_not_found_total";
    private static final String LOGIN_FAILURE_WRONG_PASSWORD_TOTAL = METRIC_PREFIX + "login_failure_wrong_password_total";
    private static final String LOGOUT_SUCCESS_TOTAL = METRIC_PREFIX + "logout_success_total";
    private static final String AUTH_OPERATION_ERRORS_TOTAL = METRIC_PREFIX + "operation_errors_total";

    public AuthStatisticsServiceImpl(PrometheusQueryClient prometheusClient, ObjectMapper objectMapper) {
        super(prometheusClient, objectMapper);
    }

    @Override
    public Mono<AuthSummaryStatsDTO> getAuthSummary(StatisticsQueryRequestDTO request) {
        String range = "[" + request.getTimeRange() + "]";

        Mono<Long> loginSuccess = queryScalar(LOGIN_SUCCESS_TOTAL, range, "Login Success");
        Mono<Long> loginFailUser = queryScalar(LOGIN_FAILURE_USER_NOT_FOUND_TOTAL, range, "Login Fail UserNotFound");
        Mono<Long> loginFailPass = queryScalar(LOGIN_FAILURE_WRONG_PASSWORD_TOTAL, range, "Login Fail WrongPass");
        Mono<Long> logoutSuccess = queryScalar(LOGOUT_SUCCESS_TOTAL, range, "Logout Success");
        Mono<Long> totalErrors = queryScalar(AUTH_OPERATION_ERRORS_TOTAL, range, "Auth Errors");

        return Mono.zip(loginSuccess, loginFailUser, loginFailPass, logoutSuccess, totalErrors)
                .map(tuple -> AuthSummaryStatsDTO.builder()
                        .timeRange(request.getTimeRange())
                        .loginSuccessCount(tuple.getT1())
                        .loginFailureUserNotFoundCount(tuple.getT2())
                        .loginFailureWrongPasswordCount(tuple.getT3())
                        .logoutSuccessCount(tuple.getT4())
                        .totalAuthErrors(tuple.getT5())
                        .build());
    }
}
