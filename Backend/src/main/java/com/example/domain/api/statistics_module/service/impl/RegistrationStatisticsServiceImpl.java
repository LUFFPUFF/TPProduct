package com.example.domain.api.statistics_module.service.impl;

import com.example.domain.api.statistics_module.metrics.client.PrometheusQueryClient;
import com.example.domain.api.statistics_module.model.auth.RegistrationSummaryStatsDTO;
import com.example.domain.api.statistics_module.model.metric.StatisticsQueryRequestDTO;
import com.example.domain.api.statistics_module.service.AbstractStatisticsService;
import com.example.domain.api.statistics_module.service.IRegistrationStatisticsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public class RegistrationStatisticsServiceImpl extends AbstractStatisticsService implements IRegistrationStatisticsService {

    private static final String METRIC_PREFIX = "registration_app_";
    private static final String REG_ATTEMPT_TOTAL = METRIC_PREFIX + "attempt_total";
    private static final String REG_CODE_SENT_SUCCESS_TOTAL = METRIC_PREFIX + "code_sent_success_total";
    private static final String REG_CODE_SENT_FAILURE_TOTAL = METRIC_PREFIX + "code_sent_failure_total";
    private static final String REG_CODE_CHECK_SUCCESS_TOTAL = METRIC_PREFIX + "code_check_success_total";
    private static final String REG_CODE_CHECK_FAILURE_INVALID_TOTAL = METRIC_PREFIX + "code_check_failure_invalid_total";
    private static final String REG_EMAIL_EXISTS_TOTAL = METRIC_PREFIX + "email_exists_total";
    private static final String REG_OPERATION_ERRORS_TOTAL = METRIC_PREFIX + "operation_errors_total";

    public RegistrationStatisticsServiceImpl(PrometheusQueryClient prometheusClient, ObjectMapper objectMapper) {
        super(prometheusClient, objectMapper);
    }

    @Override
    public Mono<RegistrationSummaryStatsDTO> getRegistrationSummary(StatisticsQueryRequestDTO request) {
        String range = "[" + request.getTimeRange() + "]";
        log.info("Requesting registration summary, timeRange: {}", request.getTimeRange());

        Mono<Long> attempts = queryScalar(REG_ATTEMPT_TOTAL, range, "Registration Attempts");
        Mono<Long> codeSentSuccess = queryScalar(REG_CODE_SENT_SUCCESS_TOTAL, range, "Code Sent Success");
        Mono<Long> codeSentFailure = queryScalar(REG_CODE_SENT_FAILURE_TOTAL, range, "Code Sent Failure");
        Mono<Long> codeCheckSuccess = queryScalar(REG_CODE_CHECK_SUCCESS_TOTAL, range, "Code Check Success");
        Mono<Long> codeCheckFailure = queryScalar(REG_CODE_CHECK_FAILURE_INVALID_TOTAL, range, "Code Check Failure Invalid");
        Mono<Long> emailExists = queryScalar(REG_EMAIL_EXISTS_TOTAL, range, "Email Exists on Reg");
        Mono<Long> totalErrors = queryScalar(REG_OPERATION_ERRORS_TOTAL, range, "Total Reg Errors");

        return Mono.zip(attempts, codeSentSuccess, codeSentFailure, codeCheckSuccess, codeCheckFailure, emailExists, totalErrors)
                .map(tuple -> RegistrationSummaryStatsDTO.builder()
                        .timeRange(request.getTimeRange())
                        .registrationAttempts(tuple.getT1())
                        .codesSentSuccess(tuple.getT2())
                        .codesSentFailure(tuple.getT3())
                        .codesCheckedSuccess(tuple.getT4())
                        .codesCheckedFailureInvalid(tuple.getT5())
                        .emailExistsDuringRegistration(tuple.getT6())
                        .totalRegistrationErrors(tuple.getT7())
                        .build())
                .doOnSuccess(summary -> log.info("Built registration summary: {}", summary))
                .doOnError(e -> log.error("Error building registration summary for request {}: {}", request, e.getMessage(), e));
    }
}
