package com.example.domain.api.statistics_module.service.impl;

import com.example.database.model.company_subscription_module.user_roles.user.Role;
import com.example.domain.api.statistics_module.metrics.client.PrometheusQueryClient;
import com.example.domain.api.statistics_module.model.auth.RoleSummaryStatsDTO;
import com.example.domain.api.statistics_module.model.metric.StatisticsQueryRequestDTO;
import com.example.domain.api.statistics_module.service.AbstractStatisticsService;
import com.example.domain.api.statistics_module.service.IRoleStatisticsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class RoleStatisticsServiceImpl extends AbstractStatisticsService implements IRoleStatisticsService {

    private static final String METRIC_PREFIX = "role_app_";
    private static final String ROLE_ADDED_SUCCESS_TOTAL = METRIC_PREFIX + "added_success_total";
    private static final String ROLE_REMOVED_SUCCESS_TOTAL = METRIC_PREFIX + "removed_success_total";
    private static final String ROLE_OP_FAILURE_USER_NOT_FOUND_TOTAL = METRIC_PREFIX + "operation_failure_user_not_found_total";
    private static final String ROLE_OPERATION_ERRORS_TOTAL = METRIC_PREFIX + "operation_errors_total";

    private static final String TAG_ROLE_NAME = "role_name";

    public RoleStatisticsServiceImpl(PrometheusQueryClient prometheusClient, ObjectMapper objectMapper) {
        super(prometheusClient, objectMapper);
    }

    @Override
    public Mono<RoleSummaryStatsDTO> getRoleSummary(StatisticsQueryRequestDTO request) {
        String range = "[" + request.getTimeRange() + "]";
        log.info("Requesting role summary, timeRange: {}", request.getTimeRange());

        List<Role> allPossibleRoles = Arrays.asList(Role.values());

        Mono<Map<String, Long>> rolesAddedMapMono = Flux.fromIterable(allPossibleRoles)
                .flatMap(role -> {
                    String filter = String.format("{%s=\"%s\"}", TAG_ROLE_NAME, role.name());
                    String promqlQuery = String.format("sum(increase(%s%s%s))", ROLE_ADDED_SUCCESS_TOTAL, filter, range);
                    return querySingleScalarInternal(promqlQuery, "Role Added: " + role.name())
                            .map(count -> Map.entry(role.name(), count));
                })
                .filter(entry -> entry.getValue() > 0)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        Mono<Map<String, Long>> rolesRemovedMapMono = Flux.fromIterable(allPossibleRoles)
                .flatMap(role -> {
                    String filter = String.format("{%s=\"%s\"}", TAG_ROLE_NAME, role.name());
                    String promqlQuery = String.format("sum(increase(%s%s%s))", ROLE_REMOVED_SUCCESS_TOTAL, filter, range);
                    return querySingleScalarInternal(promqlQuery, "Role Removed: " + role.name())
                            .map(count -> Map.entry(role.name(), count));
                })
                .filter(entry -> entry.getValue() > 0)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        Mono<Long> userNotFoundFailures = queryScalar(ROLE_OP_FAILURE_USER_NOT_FOUND_TOTAL, range, "User Not Found for Role Op");
        Mono<Long> totalErrors = queryScalar(ROLE_OPERATION_ERRORS_TOTAL, range, "Total Role Errors");

        return Mono.zip(
                        rolesAddedMapMono.defaultIfEmpty(new HashMap<>()),
                        rolesRemovedMapMono.defaultIfEmpty(new HashMap<>()),
                        userNotFoundFailures,
                        totalErrors
                )
                .map(tuple -> RoleSummaryStatsDTO.builder()
                        .timeRange(request.getTimeRange())
                        .rolesAddedCount(tuple.getT1())
                        .rolesRemovedCount(tuple.getT2())
                        .userNotFoundFailures(tuple.getT3())
                        .totalRoleOperationErrors(tuple.getT4())
                        .build())
                .doOnSuccess(summary -> log.info("Built role summary: {}", summary))
                .doOnError(e -> log.error("Error building role summary for request {}: {}", request, e.getMessage(), e));
    }
}
