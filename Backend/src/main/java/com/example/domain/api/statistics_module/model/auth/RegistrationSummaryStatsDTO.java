package com.example.domain.api.statistics_module.model.auth;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RegistrationSummaryStatsDTO {
    private String timeRange;
    private long registrationAttempts;
    private long codesSentSuccess;
    private long codesSentFailure;
    private long codesCheckedSuccess;
    private long codesCheckedFailureInvalid;
    private long emailExistsDuringRegistration;
    private long totalRegistrationErrors;
}
