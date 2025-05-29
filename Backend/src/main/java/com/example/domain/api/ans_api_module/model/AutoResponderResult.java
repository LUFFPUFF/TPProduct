package com.example.domain.api.ans_api_module.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AutoResponderResult {

    private boolean answerProvided;
    private String responseText;
    private boolean requiresEscalation;
    private String escalationReasonMessageKey;
    private boolean isPurelyGenerated;
    private Float confidence;
}
