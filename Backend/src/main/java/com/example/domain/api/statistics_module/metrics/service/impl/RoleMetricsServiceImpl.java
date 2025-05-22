package com.example.domain.api.statistics_module.metrics.service.impl;

import com.example.database.model.company_subscription_module.user_roles.user.Role;
import com.example.domain.api.statistics_module.metrics.service.IRoleMetricsService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RoleMetricsServiceImpl implements IRoleMetricsService {

    private final MeterRegistry registry;
    private static final String METRIC_PREFIX = "role_app_";

    private static final String ROLE_ADDED_SUCCESS_TOTAL = METRIC_PREFIX + "added_success_total";
    private static final String ROLE_REMOVED_SUCCESS_TOTAL = METRIC_PREFIX + "removed_success_total";
    private static final String ROLE_OP_FAILURE_USER_NOT_FOUND_TOTAL = METRIC_PREFIX + "operation_failure_user_not_found_total";
    private static final String ROLE_OPERATION_ERRORS_TOTAL = METRIC_PREFIX + "operation_errors_total";

    private static final String TAG_ROLE_NAME = "role_name";
    private static final String TAG_OPERATION_NAME = "operation";
    private static final String TAG_ERROR_TYPE = "error_type";

    private String sanitizeRoleName(Role role) {
        return role != null ? role.name() : "unknown";
    }
    private String sanitizeRoleName(String roleName) {
        return roleName != null ? roleName : "unknown";
    }

    @Override
    public void incrementRoleAddedSuccess(String roleName) {
        Counter.builder(ROLE_ADDED_SUCCESS_TOTAL)
                .tag(TAG_ROLE_NAME, sanitizeRoleName(roleName))
                .register(registry)
                .increment();
    }

    @Override
    public void incrementRoleRemovedSuccess(String roleName) {
        Counter.builder(ROLE_REMOVED_SUCCESS_TOTAL)
                .tag(TAG_ROLE_NAME, sanitizeRoleName(roleName))
                .register(registry)
                .increment();
    }

    @Override
    public void incrementRoleOperationFailureUserNotFound(String operation, String roleName) {
        Counter.builder(ROLE_OP_FAILURE_USER_NOT_FOUND_TOTAL)
                .tag(TAG_OPERATION_NAME, operation)
                .tag(TAG_ROLE_NAME, sanitizeRoleName(roleName))
                .register(registry)
                .increment();
    }

    @Override
    public void incrementRoleOperationError(String operationName, String roleName, String errorType) {
        Counter.builder(ROLE_OPERATION_ERRORS_TOTAL)
                .tag(TAG_OPERATION_NAME, operationName)
                .tag(TAG_ROLE_NAME, sanitizeRoleName(roleName))
                .tag(TAG_ERROR_TYPE, errorType)
                .register(registry)
                .increment();
    }
}
